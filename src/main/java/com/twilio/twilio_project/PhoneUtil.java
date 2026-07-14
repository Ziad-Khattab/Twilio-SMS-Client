package com.twilio.twilio_project;

public final class PhoneUtil {

    private PhoneUtil() {
    }

    public static String normalize(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        String normalized = phone.replaceAll("[\\s\\-()]+", "");
        if (!normalized.startsWith("+")) {
            normalized = "+" + normalized;
        }
        return normalized;
    }

    public static boolean validateE164(String phone) {
        return phone != null && phone.matches("^\\+\\d{5,15}$");
    }
}
