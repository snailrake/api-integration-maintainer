package io.github.snailrake.maintainer.server.config;

import io.github.snailrake.maintainer.server.model.AccountRole;
import io.github.snailrake.maintainer.server.repository.AccountRepository;
import io.github.snailrake.maintainer.server.security.AuthenticationAttemptFilter;
import io.github.snailrake.maintainer.server.security.SessionValidationFilter;
import io.github.snailrake.maintainer.server.security.SignedInAccount;
import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
class AccountSecurityConfiguration {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String PASSWORD_LINK_PATH = "/api/auth/password-link";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accountRepository) {
        return login -> accountRepository.findByLogin(login)
                .map(SignedInAccount::new)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден."));
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain accountSecurity(HttpSecurity http, AccountRepository accountRepository) {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, LOGIN_PATH, PASSWORD_LINK_PATH).permitAll()
                        .requestMatchers("/api/admin/**").hasRole(AccountRole.ADMIN.name())
                        .anyRequest().authenticated())
                .requestCache(RequestCacheConfigurer::disable)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, failure) -> response.setStatus(HttpStatus.FORBIDDEN.value())))
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl(LOGIN_PATH)
                        .usernameParameter("login")
                        .successHandler((request, response, auth) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((request, response, failure) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, auth) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
                .addFilterBefore(
                        new AuthenticationAttemptFilter(LOGIN_PATH, PASSWORD_LINK_PATH),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(new SessionValidationFilter(accountRepository), AuthorizationFilter.class)
                .build();
    }
}
