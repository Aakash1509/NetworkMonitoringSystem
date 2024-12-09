package org.example.model;

public record Credential(
        Long profileId,
        String profileName,
        String protocol,
        String userName,
        String password,
        String community,
        String version
) {}

