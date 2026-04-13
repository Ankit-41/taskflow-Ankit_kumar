package com.ankit.taskflow.idempotency;

public record IdempotencyPayload(int status, String body) {
}
