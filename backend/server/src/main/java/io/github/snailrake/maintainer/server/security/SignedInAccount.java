package io.github.snailrake.maintainer.server.security;

import java.util.List;
import java.util.Objects;

import io.github.snailrake.maintainer.server.model.AccountRole;
import io.github.snailrake.maintainer.server.model.UserAccount;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public final class SignedInAccount extends User {

    private final long id;
    private final AccountRole role;
    private final long sessionVersion;

    public SignedInAccount(UserAccount account) {
        super(
                account.login(),
                Objects.toString(account.passwordHash(), ""),
                account.canSignIn(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()))
        );

        this.id = account.id();
        this.role = account.role();
        this.sessionVersion = account.sessionVersion();
    }

    public long id() {
        return id;
    }

    public AccountRole role() {
        return role;
    }

    public long sessionVersion() {
        return sessionVersion;
    }
}
