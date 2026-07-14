package com.twilio.twilio_project;

public interface SmsProvider {
    SmsResult send(String to, String message, String from);
    SmsProviderType getType();
}
