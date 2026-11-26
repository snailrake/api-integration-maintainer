package io.github.snailrake.maintainer.server.security;

import java.io.IOException;
import java.util.Objects;

import io.github.snailrake.maintainer.server.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public final class SessionValidationFilter extends OncePerRequestFilter {

    private final AccountRepository accountRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.nonNull(authentication) &&
                authentication.getPrincipal() instanceof SignedInAccount account &&
                !accountRepository.hasValidSession(account.id(), account.sessionVersion())) {
            logoutHandler.logout(request, response, authentication);
        }

        chain.doFilter(request, response);
    }
}
