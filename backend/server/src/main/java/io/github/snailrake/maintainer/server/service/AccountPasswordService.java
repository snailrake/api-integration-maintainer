package io.github.snailrake.maintainer.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

import io.github.snailrake.maintainer.server.repository.AccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class AccountPasswordService {

    private static final int MIN_PASSWORD_LENGTH = 15;
    private static final int PASSWORD_TOKEN_BYTES = 32;
    private static final Duration PASSWORD_TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Pattern PASSWORD_TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<PasswordLink> issuePasswordLink(long accountId) {
        var tokenBytes = new byte[PASSWORD_TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        var tokenHash = hashPasswordToken(token);

        var expiresAt = accountRepository.replacePasswordLink(accountId, tokenHash, PASSWORD_TOKEN_LIFETIME.toSeconds());
        return expiresAt.map(expiry -> new PasswordLink("/set-password#token=" + token, expiry));
    }

    public ChangeOutcome setPasswordFromLink(String token, String password) {
        var validTokenHash = Optional.ofNullable(token)
                .filter(value -> PASSWORD_TOKEN_FORMAT.matcher(value).matches())
                .map(AccountPasswordService::hashPasswordToken)
                .filter(accountRepository::hasValidPasswordLink);

        var outcome = ChangeOutcome.INVALID_LINK;
        if (validTokenHash.isPresent()) {
            if (!hasSufficientLength(password)) {
                outcome = ChangeOutcome.PASSWORD_TOO_SHORT;
            } else {
                var passwordHash = passwordEncoder.encode(password);
                var installed = accountRepository.installPasswordFromLink(validTokenHash.orElseThrow(), passwordHash);
                outcome = installed ? ChangeOutcome.CHANGED : ChangeOutcome.INVALID_LINK;
            }
        }

        return outcome;
    }

    public ChangeOutcome changeOwnPassword(long accountId, long sessionVersion, String currentPassword, String newPassword) {
        var signedInAccount = accountRepository.findById(accountId)
                .filter(account -> account.canSignIn() && account.sessionVersion() == sessionVersion);

        var outcome = ChangeOutcome.SESSION_EXPIRED;
        if (signedInAccount.isPresent()) {
            var account = signedInAccount.orElseThrow();
            if (!StringUtils.hasLength(currentPassword) || !passwordEncoder.matches(currentPassword, account.passwordHash())) {
                outcome = ChangeOutcome.WRONG_CURRENT_PASSWORD;
            } else if (!hasSufficientLength(newPassword)) {
                outcome = ChangeOutcome.PASSWORD_TOO_SHORT;
            } else {
                var passwordHash = passwordEncoder.encode(newPassword);
                var changed = accountRepository.changePassword(accountId, sessionVersion, passwordHash);
                outcome = changed ? ChangeOutcome.CHANGED : ChangeOutcome.SESSION_EXPIRED;
            }
        }

        return outcome;
    }

    String hashAdministratorPassword(String password) {
        if (!StringUtils.hasLength(password)) {
            throw new IllegalStateException("Задайте MAINTAINER_ADMIN_PASSWORD для создания или восстановления администратора.");
        }
        if (!hasSufficientLength(password)) {
            throw new IllegalStateException("Пароль администратора должен содержать не менее 15 символов.");
        }

        return passwordEncoder.encode(password);
    }

    private static boolean hasSufficientLength(String password) {
        return StringUtils.hasLength(password) && password.codePointCount(0, password.length()) >= MIN_PASSWORD_LENGTH;
    }

    private static String hashPasswordToken(String token) {
        try {
            var hasher = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(hasher.digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 недоступен.", exception);
        }
    }

    public enum ChangeOutcome {
        CHANGED,
        INVALID_LINK,
        WRONG_CURRENT_PASSWORD,
        PASSWORD_TOO_SHORT,
        SESSION_EXPIRED
    }

    public record PasswordLink(String path, OffsetDateTime expiresAt) {
    }
}
