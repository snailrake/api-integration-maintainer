package io.github.snailrake.maintainer.server.model;

import java.util.Objects;

public record UserAccount(long id, String login, AccountRole role, boolean blocked, String passwordHash, long sessionVersion) {

    public static final int MAX_LOGIN_LENGTH = 128;

    public static String normalizeLogin(String login) {
        return Objects.toString(login, "").trim();
    }

    public boolean canSignIn() {
        return !blocked && Objects.nonNull(passwordHash);
    }
}
