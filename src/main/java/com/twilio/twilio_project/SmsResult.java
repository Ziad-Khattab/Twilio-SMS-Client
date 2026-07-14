package com.twilio.twilio_project;

public class SmsResult {
    private final String messageId;
    private final boolean success;
    private final String error;
    private final String providerRefId;

    public SmsResult(String messageId) {
        this.messageId = messageId;
        this.success = true;
        this.error = null;
        this.providerRefId = null;
    }

    public SmsResult(String messageId, String providerRefId) {
        this.messageId = messageId;
        this.success = true;
        this.error = null;
        this.providerRefId = providerRefId;
    }

    public SmsResult(boolean success, String error) {
        this.messageId = null;
        this.success = success;
        this.error = error;
        this.providerRefId = null;
    }

    public String getMessageId() { return messageId; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public String getProviderRefId() { return providerRefId; }
}
