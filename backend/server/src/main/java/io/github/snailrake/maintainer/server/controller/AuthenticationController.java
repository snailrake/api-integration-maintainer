package io.github.snailrake.maintainer.server.controller;

import io.github.snailrake.maintainer.server.model.AccountRole;
import io.github.snailrake.maintainer.server.security.SignedInAccount;
import io.github.snailrake.maintainer.server.service.AccountPasswordService;
import io.github.snailrake.maintainer.server.service.AccountPasswordService.ChangeOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RestController
@RequestMapping("/api/auth")
class AuthenticationController {

    private final AccountPasswordService accountPasswordService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @GetMapping("/csrf")
    CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/me")
    AccountIdentity currentAccount(@AuthenticationPrincipal SignedInAccount account) {
        return new AccountIdentity(account.id(), account.getUsername(), account.role());
    }

    @PostMapping("/password-link")
    ResponseEntity<?> setPasswordFromLink(@RequestBody SetPasswordRequest request) {
        return passwordResponse(accountPasswordService.setPasswordFromLink(request.token(), request.password()));
    }

    @PutMapping("/password")
    ResponseEntity<?> changePassword(
            @AuthenticationPrincipal SignedInAccount account,
            @RequestBody ChangePasswordRequest change,
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        var outcome = accountPasswordService.changeOwnPassword(
                account.id(),
                account.sessionVersion(),
                change.currentPassword(),
                change.newPassword()
        );
        if (outcome == ChangeOutcome.CHANGED) {
            logoutHandler.logout(request, response, authentication);
        }

        return passwordResponse(outcome);
    }

    private static ResponseEntity<?> passwordResponse(ChangeOutcome outcome) {
        return switch (outcome) {
            case CHANGED -> ResponseEntity.noContent().build();
            case INVALID_LINK -> AccountErrorHandler.problem(
                    HttpStatus.BAD_REQUEST,
                    "Ссылка недействительна. Запросите новую у администратора."
            );
            case WRONG_CURRENT_PASSWORD -> AccountErrorHandler.problem(HttpStatus.BAD_REQUEST, "Текущий пароль указан неверно.");
            case PASSWORD_TOO_SHORT -> AccountErrorHandler.problem(
                    HttpStatus.BAD_REQUEST,
                    "Пароль должен содержать не менее 15 символов."
            );
            case SESSION_EXPIRED -> AccountErrorHandler.problem(HttpStatus.UNAUTHORIZED, "Войдите в систему снова.");
        };
    }

    record AccountIdentity(long id, String login, AccountRole role) {
    }

    record SetPasswordRequest(String token, String password) {
    }

    record ChangePasswordRequest(String currentPassword, String newPassword) {
    }
}
