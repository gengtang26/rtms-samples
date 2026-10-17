package com.zoom.rtms.vsdkworkingjava.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class ZoomConfig {

    @Value("${ZOOM_CLIENT_ID:xxxxxx}")
    private String clientId;

    @Value("${ZOOM_CLIENT_SECRET:yyyyyyy}")
    private String clientSecret;

    @Value("${ZOOM_SECRET_TOKEN:zzzzzz}")
    private String secretToken;

    @Value("${RTMS_MEDIA_TYPES:}")
    private String mediaTypes;

    @PostConstruct
    public void init() {
        var supported = java.util.Set.of("audio", "video", "screenshare", "transcript", "chat");
        var selected = new java.util.HashSet<String>();
        for (String rawName : mediaTypes.split(",")) {
            String name = rawName.trim().toLowerCase();
            if (name.isEmpty() || !supported.contains(name) || !selected.add(name)) {
                throw new IllegalArgumentException(
                        "RTMS_MEDIA_TYPES must list unique supported media names; invalid value: " + name);
            }
        }
        log.info("ZoomConfig initialized:");
        log.info("  Client ID loaded: {}", clientId != null ? "YES (" + clientId.substring(0, 8) + "...)" : "NO");
        log.info("  Client Secret loaded: {}", clientSecret != null ? "YES (" + clientSecret.substring(0, 8) + "...)" : "NO");
        log.info("  Secret Token loaded: {}", secretToken != null ? "YES (" + secretToken.substring(0, 8) + "...)" : "NO");
    }
}
