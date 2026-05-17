package com.twilio.twilio_project;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public final class TwilioSmsService {

    private TwilioSmsService() {
    }

    public static Message send(String accountSid, String authToken, String from, String to, String body) {
        Twilio.init(accountSid, authToken);
        return Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
    }
}
