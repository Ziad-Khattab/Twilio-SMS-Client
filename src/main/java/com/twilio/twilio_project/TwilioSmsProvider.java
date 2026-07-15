package com.twilio.twilio_project; // Twilio SmsProvider adapter — delegates send to Twilio REST API

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

// Adapter that wraps the Twilio REST API into the shape SmsRouter expects.
// Twilio.init() must be called by the caller before invoking send() —
// this class does not manage Twilio credentials itself.
// If TWILIO_STATUS_CALLBACK_URL env var is set, attaches StatusCallback for delivery receipts.
public class TwilioSmsProvider {

    public SmsResult send(String to, String message, String from) {
        try {
            MessageCreator creator = Message.creator(new PhoneNumber(to), new PhoneNumber(from), message);
            String cb = EnvLoader.get("TWILIO_STATUS_CALLBACK_URL");
            if (cb != null && !cb.isEmpty()) {
                creator.setStatusCallback(cb);
            }
            Message msg = creator.create();
            return new SmsResult(msg.getSid(), msg.getSid());
        } catch (Exception e) {
            return new SmsResult(false, "Twilio error: " + e.getMessage());
        }
    }
}
