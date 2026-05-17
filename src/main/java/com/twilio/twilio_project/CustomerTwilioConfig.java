package com.twilio.twilio_project;

public class CustomerTwilioConfig {

    private final String accountSid;
    private final String authToken;
    private final String senderId;

    public CustomerTwilioConfig(String accountSid, String authToken, String senderId) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.senderId = senderId;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getSenderId() {
        return senderId;
    }

    public boolean isComplete() {
        return accountSid != null && !accountSid.isEmpty()
                && authToken != null && !authToken.isEmpty()
                && senderId != null && !senderId.isEmpty();
    }
}
