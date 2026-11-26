package io.github.snailrake.maintainer.server.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Тест для AuthenticationController")
class AccountFlowIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.7")
            .withStartupTimeout(Duration.ofMinutes(3));

    private static final JsonMapper JSON = new JsonMapper();
    private static final String ADMIN_LOGIN = "fixture-admin";
    private static final String ADMIN_PASSWORD = "Fixture administrator password";
    private static final String INITIAL_PASSWORD = "Fixture initial password";
    private static final String RESET_PASSWORD = "Fixture reset password";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient database;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("maintainer.admin.login", () -> ADMIN_LOGIN);
        registry.add("maintainer.admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    @DisplayName("Когда одну ссылку отправляют дважды одновременно, тогда пароль устанавливается один раз")
    void setPassword_whenLinkIsUsedConcurrently_thenOnlyOneRequestSucceeds() {
        // Given
        try (var admin = browser(); var firstUser = browser(); var secondUser = browser()) {
            signInAsAdministrator(admin);
            var created = createDeveloper(admin, "developer-parallel");
            var token = tokenFrom(created.path("passwordLink"));
            var weakPassword = installPassword(firstUser, token, "короткий");
            assertThat(weakPassword.statusCode()).isEqualTo(400);
            assertThat(weakPassword.body()).contains("не менее 15 символов");

            // When
            var first = CompletableFuture.supplyAsync(() -> installPassword(firstUser, token, INITIAL_PASSWORD));
            var second = CompletableFuture.supplyAsync(() -> installPassword(secondUser, token, INITIAL_PASSWORD));

            // Then
            assertThat(List.of(first.join().statusCode(), second.join().statusCode())).containsExactlyInAnyOrder(204, 400);
            assertThat(login(firstUser, "developer-parallel", INITIAL_PASSWORD)).isEqualTo(204);
            assertThat(get(firstUser, "/api/admin/users").statusCode()).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("Когда пользователь устанавливает новый пароль по ссылке, тогда старый пароль и сеансы отзываются")
    void resetPassword_whenNewPasswordIsInstalled_thenOldCredentialsAndSessionsExpire() {
        // Given
        try (var admin = browser(); var developer = browser(); var secondSession = browser(); var recipient = browser()) {
            signInAsAdministrator(admin);
            var created = createDeveloper(admin, "developer-reset");
            var id = created.path("account").path("id").asLong();
            var initialToken = tokenFrom(created.path("passwordLink"));
            assertThat(installPassword(recipient, initialToken, INITIAL_PASSWORD).statusCode()).isEqualTo(204);
            var supersededLink = issuePasswordLink(admin, id);
            var currentLink = issuePasswordLink(admin, id);
            assertThat(installPassword(recipient, tokenFrom(supersededLink), RESET_PASSWORD).statusCode()).isEqualTo(400);
            assertThat(login(developer, "developer-reset", INITIAL_PASSWORD)).isEqualTo(204);
            assertThat(login(secondSession, "developer-reset", INITIAL_PASSWORD)).isEqualTo(204);

            // When
            assertThat(installPassword(recipient, tokenFrom(currentLink), RESET_PASSWORD).statusCode()).isEqualTo(204);

            // Then
            assertThat(get(developer, "/api/auth/me").statusCode()).isEqualTo(401);
            assertThat(get(secondSession, "/api/auth/me").statusCode()).isEqualTo(401);
            assertThat(login(developer, "developer-reset", INITIAL_PASSWORD)).isEqualTo(401);
            assertThat(login(developer, "developer-reset", RESET_PASSWORD)).isEqualTo(204);
        }
    }

    @Test
    @DisplayName("Когда срок действия ссылки истёк, тогда установить пароль по ней нельзя")
    void setPassword_whenLinkExpires_thenAccountRemainsWithoutPassword() {
        // Given
        try (var admin = browser(); var recipient = browser()) {
            signInAsAdministrator(admin);
            var created = createDeveloper(admin, "developer-expired");
            var id = created.path("account").path("id").asLong();
            database.sql("""
                            UPDATE user_account
                            SET password_token_expires_at = clock_timestamp() - INTERVAL '1 second'
                            WHERE id = :id
                            """)
                    .param("id", id)
                    .update();

            // When
            var response = installPassword(recipient, tokenFrom(created.path("passwordLink")), INITIAL_PASSWORD);

            // Then
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(login(recipient, "developer-expired", INITIAL_PASSWORD)).isEqualTo(401);
        }
    }

    @Test
    @DisplayName("Когда администратор меняет роль и блокирует учётную запись, тогда прежние сеансы и ссылка недействительны")
    void updateAccess_whenRoleOrBlockChanges_thenOldSessionsAndLinkExpire() {
        // Given
        try (var admin = browser(); var developer = browser(); var recipient = browser()) {
            signInAsAdministrator(admin);
            var created = createDeveloper(admin, "developer-access");
            var id = created.path("account").path("id").asLong();
            var initialToken = tokenFrom(created.path("passwordLink"));
            assertThat(installPassword(recipient, initialToken, INITIAL_PASSWORD).statusCode()).isEqualTo(204);
            assertThat(login(developer, "developer-access", INITIAL_PASSWORD)).isEqualTo(204);

            // When
            var changedRole = writeJson(admin, "PUT", "/api/admin/users/" + id + "/role", Map.of("role", "ADMIN"));
            assertThat(changedRole.statusCode()).isEqualTo(200);
            assertThat(get(developer, "/api/auth/me").statusCode()).isEqualTo(401);
            assertThat(login(developer, "developer-access", INITIAL_PASSWORD)).isEqualTo(204);
            assertThat(get(developer, "/api/admin/users").statusCode()).isEqualTo(200);
            var link = issuePasswordLink(admin, id);
            assertThat(writeJson(admin, "PUT", "/api/admin/users/" + id + "/blocked", Map.of("blocked", true)).statusCode())
                    .isEqualTo(200);

            // Then
            assertThat(get(developer, "/api/auth/me").statusCode()).isEqualTo(401);
            assertThat(login(developer, "developer-access", INITIAL_PASSWORD)).isEqualTo(401);
            assertThat(installPassword(recipient, tokenFrom(link), RESET_PASSWORD).statusCode()).isEqualTo(400);

            var unblocked = writeJson(admin, "PUT", "/api/admin/users/" + id + "/blocked", Map.of("blocked", false));
            assertThat(unblocked.statusCode()).isEqualTo(200);
            assertThat(get(developer, "/api/auth/me").statusCode()).isEqualTo(401);
            assertThat(login(developer, "developer-access", INITIAL_PASSWORD)).isEqualTo(204);
        }
    }

    @Test
    @DisplayName("Когда администратор повторно создаёт пользователя с тем же логином, тогда система сообщает о конфликте")
    void createUser_whenLoginAlreadyExists_thenReturnsConflict() {
        // Given
        try (var admin = browser()) {
            signInAsAdministrator(admin);
            var login = "developer-duplicate";
            createDeveloper(admin, login);

            // When
            var response = writeJson(admin, "POST", "/api/admin/users", Map.of("login", login, "role", "ADMIN"));

            // Then
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(JSON.readTree(response.body()).path("detail").asString()).isEqualTo("Этот логин уже используется.");
            var storedRole = database.sql("SELECT role FROM user_account WHERE login = :login")
                    .param("login", login)
                    .query(String.class)
                    .single();
            assertThat(storedRole).isEqualTo("DEVELOPER");
        }
    }

    @Test
    @DisplayName("Когда пользователь успешно входит несколько раз, тогда попытки не расходуют лимит")
    void login_whenCredentialsAreValid_thenRateLimitDoesNotAccumulate() {
        // Given
        try (var client = browser()) {
            for (var attempt = 1; attempt <= 32; attempt++) {
                // When
                var status = login(client, ADMIN_LOGIN, ADMIN_PASSWORD);

                // Then
                assertThat(status).as("Вход номер %s", attempt).isEqualTo(204);
                assertThat(write(client, "POST", "/api/auth/logout", "application/x-www-form-urlencoded", "").statusCode())
                        .isEqualTo(204);
            }
        }
    }

    private void signInAsAdministrator(HttpClient admin) {
        assertThat(login(admin, ADMIN_LOGIN, ADMIN_PASSWORD)).isEqualTo(204);
    }

    private HttpClient browser() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private int login(HttpClient client, String login, String password) {
        var form = "login=" + URLEncoder.encode(login, StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        return write(client, "POST", "/api/auth/login", "application/x-www-form-urlencoded", form).statusCode();
    }

    private JsonNode createDeveloper(HttpClient client, String login) {
        var response = writeJson(client, "POST", "/api/admin/users", Map.of("login", login, "role", "DEVELOPER"));
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body());
    }

    private JsonNode issuePasswordLink(HttpClient client, long id) {
        var response = write(client, "POST", "/api/admin/users/" + id + "/password-link", "application/json", "");
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private String tokenFrom(JsonNode link) {
        return link.path("path").asString().substring("/set-password#token=".length());
    }

    private HttpResponse<String> installPassword(HttpClient client, String token, String password) {
        return writeJson(client, "POST", "/api/auth/password-link", Map.of("token", token, "password", password));
    }

    private HttpResponse<String> get(HttpClient client, String path) {
        return send(client, request(path).GET().build());
    }

    private HttpResponse<String> writeJson(HttpClient client, String method, String path, Map<String, ?> data) {
        return write(client, method, path, "application/json", JSON.writeValueAsString(data));
    }

    private HttpResponse<String> write(HttpClient client, String method, String path, String contentType, String body) {
        var csrf = get(client, "/api/auth/csrf");
        assertThat(csrf.statusCode()).isEqualTo(200);
        var token = JSON.readTree(csrf.body());
        var request = request(path)
                .header("Content-Type", contentType)
                .header(token.path("headerName").asString(), token.path("token").asString())
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(client, request);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15));
    }

    private HttpResponse<String> send(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
