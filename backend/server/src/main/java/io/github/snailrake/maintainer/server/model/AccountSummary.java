package io.github.snailrake.maintainer.server.model;

public record AccountSummary(long id, String login, AccountRole role, boolean blocked, boolean passwordSet) {
}
