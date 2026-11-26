package io.github.snailrake.maintainer.server.service;

import java.util.List;
import java.util.Optional;

import io.github.snailrake.maintainer.server.model.AccountRole;
import io.github.snailrake.maintainer.server.model.AccountSummary;
import io.github.snailrake.maintainer.server.model.UserAccount;
import io.github.snailrake.maintainer.server.repository.AccountRepository;
import io.github.snailrake.maintainer.server.service.AccountPasswordService.PasswordLink;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountPasswordService accountPasswordService;

    public List<AccountSummary> listAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public Optional<CreatedAccount> createAccount(String login, AccountRole role) {
        var savedAccount = accountRepository.createAccount(login, role, null);
        var createdAccount = Optional.<CreatedAccount>empty();
        if (savedAccount.isPresent()) {
            var account = savedAccount.orElseThrow();
            var passwordLink = accountPasswordService.issuePasswordLink(account.id())
                    .orElseThrow(() -> new IllegalStateException("Не удалось выдать ссылку для новой учётной записи."));
            createdAccount = Optional.of(new CreatedAccount(account, passwordLink));
        }

        return createdAccount;
    }

    public Optional<AccountSummary> changeRole(long accountId, AccountRole role) {
        return accountRepository.changeRole(accountId, role);
    }

    public Optional<AccountSummary> changeBlocked(long accountId, boolean blocked) {
        return accountRepository.changeBlocked(accountId, blocked);
    }

    public boolean hasAccount(long accountId) {
        return accountRepository.existsById(accountId);
    }

    @Transactional
    public Optional<Long> initializeAdministrator(String configuredLogin, String configuredPassword) {
        var administratorId = Optional.<Long>empty();
        if (!accountRepository.hasAccounts()) {
            accountRepository.lockForInitialization();
            if (!accountRepository.hasAccounts()) {
                var login = requireAdministratorLogin(configuredLogin);
                var passwordHash = accountPasswordService.hashAdministratorPassword(configuredPassword);
                var administrator = accountRepository.createAccount(login, AccountRole.ADMIN, passwordHash)
                        .orElseThrow(() -> new IllegalStateException("Не удалось создать первого администратора."));
                administratorId = Optional.of(administrator.id());
            }
        }

        return administratorId;
    }

    public long recoverAdministrator(String configuredLogin, String configuredPassword) {
        var login = requireAdministratorLogin(configuredLogin);
        var passwordHash = accountPasswordService.hashAdministratorPassword(configuredPassword);
        return accountRepository.recoverAdministrator(login, passwordHash)
                .orElseThrow(() -> new IllegalStateException(
                        "Этот логин уже принадлежит разработчику. Укажите логин администратора."
                ));
    }

    private static String requireAdministratorLogin(String configuredLogin) {
        var login = UserAccount.normalizeLogin(configuredLogin);
        if (!StringUtils.hasText(login)) {
            throw new IllegalStateException("Задайте MAINTAINER_ADMIN_LOGIN для создания или восстановления администратора.");
        }
        if (login.length() > UserAccount.MAX_LOGIN_LENGTH) {
            throw new IllegalStateException("Логин администратора может содержать до 128 символов.");
        }

        return login;
    }

    public record CreatedAccount(AccountSummary account, PasswordLink passwordLink) {
    }
}
