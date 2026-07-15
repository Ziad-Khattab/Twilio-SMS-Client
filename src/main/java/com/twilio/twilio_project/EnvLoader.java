package com.twilio.twilio_project; // Load .env file — config for local dev vs Docker profiles

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import java.nio.file.Files;
import java.nio.file.Paths;

// Profile-aware env config. Supports LOCAL_/DOCKER_ key prefixes so a single .env holds both profiles.
// Fallback chain: profile-prefixed key → bare key → System.getenv.
// Looks for .env in multiple directories to work from IDE and CLI.
public class EnvLoader {
    private static final Dotenv dotenv;

    static {
        DotenvBuilder builder = Dotenv.configure().ignoreIfMissing();
        String[] candidates = {
            System.getProperty("user.dir"),
            Paths.get("").toAbsolutePath().toString(),
            System.getProperty("user.dir") + "/../Twilio-SMS-Client",
        };
        for (String dir : candidates) {
            if (dir != null && Files.exists(Paths.get(dir, ".env"))) {
                builder.directory(dir);
                break;
            }
        }
        dotenv = builder.load();
    }

    public static String get(String key) {
        return resolveForProfile(key, getProfile());
    }

    // Read APP_PROFILE from system property → .env → OS env. Defaults to "local".
    private static String getProfile() {
        String p = System.getProperty("app.profile");
        if (p == null) p = dotenv.get("APP_PROFILE");
        if (p == null) p = System.getenv("APP_PROFILE");
        return "docker".equalsIgnoreCase(p) ? "docker" : "local";
    }

    // Try DOCKER_<KEY> or LOCAL_<KEY> first, fallback to bare <KEY>.
    private static String resolveForProfile(String key, String profile) {
        String prefix = "docker".equals(profile) ? "DOCKER_" : "LOCAL_";
        String profileKey = prefix + key;
        String value = dotenv.get(profileKey);
        if (value != null) return value;
        value = System.getenv(profileKey);
        if (value != null && !value.isEmpty()) return value;
        value = dotenv.get(key);
        if (value != null) return value;
        return System.getenv(key);
    }

    public static String getProfileName() {
        return getProfile();
    }
}
