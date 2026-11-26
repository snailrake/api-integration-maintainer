package io.github.snailrake.maintainer.server.repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import io.github.snailrake.maintainer.server.model.AccountRole;
import io.github.snailrake.maintainer.server.model.AccountSummary;
import io.github.snailrake.maintainer.server.model.UserAccount;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Repository
public class AccountRepository {

    private static final String SELECT_ACCOUNT = """
            SELECT id, login, role, blocked, password_hash, session_version
            FROM user_account
            """;
    private static final String SELECT_ACCOUNT_SUMMARIES = """
            SELECT id, login, role, blocked, password_hash IS NOT NULL AS password_set
            FROM user_account
            ORDER BY login
            """;
    private static final String INSERT_ACCOUNT = """
            INSERT INTO user_account (login, role, password_hash)
            VALUES (:login, :role, :passwordHash)
            ON CONFLICT (login) DO NOTHING
            RETURNING id, login, role, blocked, password_hash IS NOT NULL AS password_set
            """;
    private static final String RECOVER_ADMINISTRATOR = """
            INSERT INTO user_account (login, role, password_hash)
            VALUES (:login, 'ADMIN', :passwordHash)
            ON CONFLICT (login) DO UPDATE
            SET blocked = FALSE,
                password_hash = EXCLUDED.password_hash,
                password_token_hash = NULL,
                password_token_expires_at = NULL,
                session_version = user_account.session_version + 1
            WHERE user_account.role = 'ADMIN'
            RETURNING id
            """;
    private static final String HAS_VALID_SESSION = """
            SELECT EXISTS (
                SELECT 1 FROM user_account
                WHERE id = :id AND session_version = :sessionVersion AND NOT blocked AND password_hash IS NOT NULL
            )
            """;
    private static final String REPLACE_PASSWORD_LINK = """
            UPDATE user_account
            SET password_token_hash = :tokenHash,
                password_token_expires_at = clock_timestamp() + :lifetimeSeconds * INTERVAL '1 second'
            WHERE id = :id AND NOT blocked
            RETURNING password_token_expires_at
            """;
    private static final String HAS_VALID_PASSWORD_LINK = """
            SELECT EXISTS (
                SELECT 1 FROM user_account
                WHERE password_token_hash = :tokenHash AND password_token_expires_at > clock_timestamp() AND NOT blocked
            )
            """;
    private static final String UPDATE_PASSWORD = """
            UPDATE user_account
            SET password_hash = :passwordHash,
                password_token_hash = NULL,
                password_token_expires_at = NULL,
                session_version = session_version + 1
            """;
    private static final String INSTALL_PASSWORD_FROM_LINK = UPDATE_PASSWORD + """
            WHERE password_token_hash = :tokenHash AND password_token_expires_at > clock_timestamp() AND NOT blocked
            """;
    private static final String UPDATE_ROLE = """
            UPDATE user_account
            SET role = :role,
                session_version = session_version + CASE WHEN role <> :role THEN 1 ELSE 0 END
            WHERE id = :id
            RETURNING id, login, role, blocked, password_hash IS NOT NULL AS password_set
            """;
    private static final String UPDATE_BLOCKED_STATUS = """
            UPDATE user_account
            SET blocked = :blocked,
                session_version = session_version + CASE WHEN blocked <> :blocked THEN 1 ELSE 0 END,
                password_token_hash = CASE WHEN :blocked THEN NULL ELSE password_token_hash END,
                password_token_expires_at = CASE WHEN :blocked THEN NULL ELSE password_token_expires_at END
            WHERE id = :id
            RETURNING id, login, role, blocked, password_hash IS NOT NULL AS password_set
            """;

    private final JdbcClient jdbcClient;

    public boolean hasAccounts() {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM user_account)").query(Boolean.class).single();
    }

    public void lockForInitialization() {
        jdbcClient.sql("LOCK TABLE user_account IN SHARE ROW EXCLUSIVE MODE").update();
    }

    public Optional<AccountSummary> createAccount(String login, AccountRole role, String passwordHash) {
        return jdbcClient.sql(INSERT_ACCOUNT)
                .param("login", login)
                .param("role", role.name())
                .param("passwordHash", passwordHash, Types.VARCHAR)
                .query(AccountSummary.class)
                .optional();
    }

    public Optional<Long> recoverAdministrator(String login, String passwordHash) {
        return jdbcClient.sql(RECOVER_ADMINISTRATOR)
                .param("login", login)
                .param("passwordHash", passwordHash)
                .query(Long.class)
                .optional();
    }

    public Optional<UserAccount> findByLogin(String login) {
        return jdbcClient.sql(SELECT_ACCOUNT + "WHERE login = :login")
                .param("login", login)
                .query(UserAccount.class)
                .optional();
    }

    public Optional<UserAccount> findById(long id) {
        return jdbcClient.sql(SELECT_ACCOUNT + "WHERE id = :id")
                .param("id", id)
                .query(UserAccount.class)
                .optional();
    }

    public boolean existsById(long id) {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM user_account WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single();
    }

    public List<AccountSummary> findAll() {
        return jdbcClient.sql(SELECT_ACCOUNT_SUMMARIES)
                .query(AccountSummary.class)
                .list();
    }

    public boolean hasValidSession(long id, long sessionVersion) {
        return jdbcClient.sql(HAS_VALID_SESSION)
                .param("id", id)
                .param("sessionVersion", sessionVersion)
                .query(Boolean.class)
                .single();
    }

    public Optional<OffsetDateTime> replacePasswordLink(long id, String tokenHash, long lifetimeSeconds) {
        return jdbcClient.sql(REPLACE_PASSWORD_LINK)
                .param("id", id)
                .param("tokenHash", tokenHash)
                .param("lifetimeSeconds", lifetimeSeconds)
                .query(OffsetDateTime.class)
                .optional();
    }

    public boolean hasValidPasswordLink(String tokenHash) {
        return jdbcClient.sql(HAS_VALID_PASSWORD_LINK)
                .param("tokenHash", tokenHash)
                .query(Boolean.class)
                .single();
    }

    public boolean installPasswordFromLink(String tokenHash, String passwordHash) {
        return jdbcClient.sql(INSTALL_PASSWORD_FROM_LINK)
                .param("tokenHash", tokenHash)
                .param("passwordHash", passwordHash)
                .update() == 1;
    }

    public boolean changePassword(long id, long sessionVersion, String passwordHash) {
        return jdbcClient.sql(UPDATE_PASSWORD + "WHERE id = :id AND session_version = :sessionVersion AND NOT blocked")
                .param("id", id)
                .param("sessionVersion", sessionVersion)
                .param("passwordHash", passwordHash)
                .update() == 1;
    }

    public Optional<AccountSummary> changeRole(long id, AccountRole role) {
        return jdbcClient.sql(UPDATE_ROLE)
                .param("id", id)
                .param("role", role.name())
                .query(AccountSummary.class)
                .optional();
    }

    public Optional<AccountSummary> changeBlocked(long id, boolean blocked) {
        return jdbcClient.sql(UPDATE_BLOCKED_STATUS)
                .param("id", id)
                .param("blocked", blocked)
                .query(AccountSummary.class)
                .optional();
    }
}
