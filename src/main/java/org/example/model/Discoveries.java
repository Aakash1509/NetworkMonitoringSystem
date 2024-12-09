package org.example.model;

import io.vertx.core.json.JsonArray;

public record Discoveries(
        Long discoveryId,
        Long credential_profile,
        String name,
        String ip,
        Integer port,
        JsonArray credential_profiles,
        String status
) {}
