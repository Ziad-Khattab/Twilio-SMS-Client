package com.twilio.twilio_project;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvLoader {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(System.getProperty("user.dir"))
            .ignoreIfMissing()
            .load();

    public static String get(String key) {
        String value = dotenv.get(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
}
