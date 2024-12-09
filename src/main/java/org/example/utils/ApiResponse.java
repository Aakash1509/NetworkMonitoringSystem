package org.example.utils;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class ApiResponse<T, E> {

    private final int statusCode;
    private final String message;
    private final T data;
    private final E error;

    private ApiResponse(int statusCode, String message, T data, E error) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public static <T, E> ApiResponse<T, E> success(int statusCode, String message, T data) {
        return new ApiResponse<>(statusCode, message, data, null);
    }

    public static <T, E> ApiResponse<T, E> error(int statusCode, String message, E error) {
        return new ApiResponse<>(statusCode, message, null, error);
    }

    public String toJson() {
        return Json.encodePrettily(new JsonObject()
                .put("statusCode", statusCode)
                .put("message", message)
                .put("data", data)
                .put("error", error));
    }
}

