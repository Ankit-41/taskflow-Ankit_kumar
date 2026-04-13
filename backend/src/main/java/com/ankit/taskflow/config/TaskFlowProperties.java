package com.ankit.taskflow.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "taskflow")
public class TaskFlowProperties {

    private final Security security = new Security();
    private final Cache cache = new Cache();

    @Getter
    @Setter
    public static class Security {
        private String jwtSecret;
        private long jwtExpirationHours;
    }

    @Getter
    @Setter
    public static class Cache {
        private Duration projectTtl = Duration.ofMinutes(10);
        private Duration taskListTtl = Duration.ofMinutes(5);
        private Duration idempotencyTtl = Duration.ofHours(24);
    }
}

