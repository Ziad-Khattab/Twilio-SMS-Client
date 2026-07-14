package com.twilio.twilio_project;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EnvLoader {
    private static final Dotenv dotenv;

    static {
        DotenvBuilder builder = Dotenv.configure().ignoreIfMissing();
        // Try user.dir first, then walk up to find project root
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
        String value = dotenv.get(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
}
