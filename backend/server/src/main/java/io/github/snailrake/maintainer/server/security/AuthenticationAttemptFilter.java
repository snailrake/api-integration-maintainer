package io.github.snailrake.maintainer.server.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.snailrake.maintainer.server.model.UserAccount;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public final class AuthenticationAttemptFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int ADDRESS_LIMIT = 30;
    private static final int LOGIN_LIMIT = 10;

    private final String loginPath;
    private final Set<String> limitedPaths;

    private final Cache<String, AtomicInteger> attemptsByAddress = newCounters();
    private final Cache<String, AtomicInteger> attemptsByLogin = newCounters();

    public AuthenticationAttemptFilter(String loginPath, String passwordLinkPath) {
        this.loginPath = loginPath;
        this.limitedPaths = Set.of(loginPath, passwordLinkPath);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !limitedPaths.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        var addressKey = request.getServletPath() + "|" + request.getRemoteAddr();
        var addressAttempts = attemptsByAddress.get(addressKey, ignored -> new AtomicInteger());

        if (reserveAttempt(addressAttempts, ADDRESS_LIMIT)) {
            var loginAttempts = loginCounter(request);
            var loginAllowed = loginAttempts.isEmpty() || reserveAttempt(loginAttempts.orElseThrow(), LOGIN_LIMIT);

            if (loginAllowed) {
                forwardAttempt(request, response, chain, addressAttempts, loginAttempts);
            } else {
                addressAttempts.decrementAndGet();
                rejectAttempt(response);
            }
        } else {
            rejectAttempt(response);
        }
    }

    private static void forwardAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            AtomicInteger addressAttempts,
            Optional<AtomicInteger> loginAttempts
    ) throws ServletException, IOException {
        var failedAttempt = false;
        try {
            chain.doFilter(request, response);
            var status = response.getStatus();
            failedAttempt = status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.UNAUTHORIZED.value();
        } finally {
            if (!failedAttempt) {
                addressAttempts.decrementAndGet();
                loginAttempts.ifPresent(AtomicInteger::decrementAndGet);
            }
        }
    }

    private Optional<AtomicInteger> loginCounter(HttpServletRequest request) {
        var login = loginPath.equals(request.getServletPath()) ? UserAccount.normalizeLogin(request.getParameter("login")) : "";
        return StringUtils.hasLength(login) && login.length() <= UserAccount.MAX_LOGIN_LENGTH
                ? Optional.of(attemptsByLogin.get(login, ignored -> new AtomicInteger()))
                : Optional.empty();
    }

    private static boolean reserveAttempt(AtomicInteger attempts, int limit) {
        return attempts.getAndUpdate(count -> Math.min(count + 1, limit)) < limit;
    }

    private static void rejectAttempt(HttpServletResponse response) {
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(WINDOW.toSeconds()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    private static Cache<String, AtomicInteger> newCounters() {
        return Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(WINDOW).build();
    }
}
