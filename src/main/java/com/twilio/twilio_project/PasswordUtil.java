package com.twilio.twilio_project;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean matches(String plainPassword, String passwordHash) {
        if (plainPassword == null || passwordHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
