package com.zoom.rtms.workingjava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import jakarta.annotation.PostConstruct;

@Configuration
@Getter
public class AppConfig {

    @Value("${APP_MODE:webhook}")
    private String mode;

    @Value("${APP_WEBHOOK_PATH:/webhook}")
    private String webhookPath;

    @Value("${RTMS_MEDIA_TYPES:}")
    private String mediaTypes;

    private int mediaTypesFlag;

    @PostConstruct
    public void validateMediaConfiguration() {
        var flagsByName = java.util.Map.of(
                "audio", 1,
                "video", 2,
                "screenshare", 4,
                "transcript", 8,
                "chat", 16);
        var selected = new java.util.HashSet<String>();
        mediaTypesFlag = 0;
        for (String rawName : mediaTypes.split(",")) {
            String name = rawName.trim().toLowerCase();
            if (name.isEmpty() || !selected.add(name) || !flagsByName.containsKey(name)) {
                throw new IllegalArgumentException(
                        "RTMS_MEDIA_TYPES must list unique supported media names; invalid value: " + name);
            }
            mediaTypesFlag |= flagsByName.get(name);
        }
        if (mediaTypesFlag == 0) {
            throw new IllegalArgumentException(
                    "RTMS_MEDIA_TYPES is required (for example: audio,video,transcript)");
        }
    }
}
