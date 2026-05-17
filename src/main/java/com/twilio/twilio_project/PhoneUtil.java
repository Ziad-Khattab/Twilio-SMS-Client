package com.twilio.twilio_project;

public final class PhoneUtil {

    private PhoneUtil() {
    }

    public static String normalize(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        String normalized = phone.replaceAll("\\s+", "");
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        return normalized;
    }
}
