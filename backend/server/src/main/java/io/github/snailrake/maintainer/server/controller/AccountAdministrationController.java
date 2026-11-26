package io.github.snailrake.maintainer.server.controller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.snailrake.maintainer.server.model.AccountRole;
import io.github.snailrake.maintainer.server.model.AccountSummary;
import io.github.snailrake.maintainer.server.model.UserAccount;
import io.github.snailrake.maintainer.server.security.SignedInAccount;
import io.github.snailrake.maintainer.server.service.AccountPasswordService;
import io.github.snailrake.maintainer.server.service.AccountService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RestController
@RequestMapping("/api/admin/users")
class AccountAdministrationController {

    private final AccountService accountService;
    private final AccountPasswordService accountPasswordService;

    @GetMapping
    List<AccountSummary> listUsers() {
        return accountService.listAccounts();
    }

    @PostMapping
    ResponseEntity<?> createUser(@AuthenticationPrincipal SignedInAccount admin, @RequestBody CreateUserRequest request) {
        var login = UserAccount.normalizeLogin(request.login());
        var role = request.role();
        var validationError = Optional.<String>empty();
        if (!StringUtils.hasText(login)) {
            validationError = Optional.of("Введите логин.");
        } else if (login.length() > UserAccount.MAX_LOGIN_LENGTH) {
            validationError = Optional.of("Логин может содержать до 128 символов.");
        } else if (Objects.isNull(role)) {
            validationError = Optional.of("Выберите роль пользователя.");
        }

        return validationError.isEmpty()
                ? saveNewUser(admin.id(), login, role)
                : AccountErrorHandler.problem(HttpStatus.BAD_REQUEST, validationError.orElseThrow());
    }

    private ResponseEntity<?> saveNewUser(long actorId, String login, AccountRole role) {
        var createdAccount = accountService.createAccount(login, role);
        createdAccount.ifPresent(created -> log.info(
                "Создан пользователь: actorId={}, userId={}, role={}",
                actorId,
                created.account().id(),
                created.account().role()
        ));
        return createdAccount.isPresent()
                ? ResponseEntity.status(HttpStatus.CREATED).body(createdAccount.orElseThrow())
                : AccountErrorHandler.problem(HttpStatus.CONFLICT, "Этот логин уже используется.");
    }

    @PostMapping("/{id}/password-link")
    ResponseEntity<?> issuePasswordLink(@AuthenticationPrincipal SignedInAccount admin, @PathVariable long id) {
        var link = accountPasswordService.issuePasswordLink(id);
        link.ifPresent(issued -> log.info("Выдана ссылка для установки пароля: actorId={}, userId={}", admin.id(), id));
        return link.isPresent() ? ResponseEntity.ok(link.orElseThrow()) : passwordLinkRejection(id);
    }

    private ResponseEntity<?> passwordLinkRejection(long accountId) {
        return accountService.hasAccount(accountId)
                ? AccountErrorHandler.problem(HttpStatus.CONFLICT, "Разблокируйте пользователя перед выдачей ссылки.")
                : AccountErrorHandler.accountNotFound();
    }

    @PutMapping("/{id}/role")
    ResponseEntity<?> changeRole(
            @AuthenticationPrincipal SignedInAccount admin,
            @PathVariable long id,
            @RequestBody ChangeRoleRequest request
    ) {
        return switch (request.role()) {
            case null -> AccountErrorHandler.problem(HttpStatus.BAD_REQUEST, "Выберите роль пользователя.");
            case AccountRole role -> {
                var account = accountService.changeRole(id, role);
                account.ifPresent(updated -> log.info(
                        "Назначена роль: actorId={}, userId={}, role={}",
                        admin.id(),
                        id,
                        updated.role()
                ));
                yield account.isPresent() ? ResponseEntity.ok(account.orElseThrow()) : AccountErrorHandler.accountNotFound();
            }
        };
    }

    @PutMapping("/{id}/blocked")
    ResponseEntity<?> changeBlocked(
            @AuthenticationPrincipal SignedInAccount admin,
            @PathVariable long id,
            @RequestBody ChangeBlockedRequest request
    ) {
        return switch (request.blocked()) {
            case null -> AccountErrorHandler.problem(HttpStatus.BAD_REQUEST, "Укажите, нужно ли заблокировать пользователя.");
            case Boolean blocked -> {
                var account = accountService.changeBlocked(id, blocked);
                account.ifPresent(updated -> log.info(
                        "Изменена блокировка: actorId={}, userId={}, blocked={}",
                        admin.id(),
                        id,
                        updated.blocked()
                ));
                yield account.isPresent() ? ResponseEntity.ok(account.orElseThrow()) : AccountErrorHandler.accountNotFound();
            }
        };
    }

    record CreateUserRequest(String login, AccountRole role) {
    }

    record ChangeRoleRequest(AccountRole role) {
    }

    record ChangeBlockedRequest(Boolean blocked) {
    }
}
