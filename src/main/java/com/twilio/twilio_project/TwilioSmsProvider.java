package com.twilio.twilio_project;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class TwilioSmsProvider implements SmsProvider {

    @Override
    public SmsResult send(String to, String message, String from) {
        try {
            // These are passed per-call — caller must init Twilio with proper creds
            Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), message).create();
            return new SmsResult(msg.getSid());
        } catch (Exception e) {
            return new SmsResult(false, "Twilio error: " + e.getMessage());
        }
    }

    @Override
    public SmsProviderType getType() {
        return SmsProviderType.TWILIO;
    }
}
