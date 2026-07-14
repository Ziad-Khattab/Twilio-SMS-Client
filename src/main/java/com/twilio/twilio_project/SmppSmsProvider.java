package com.twilio.twilio_project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmppSmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(SmppSmsProvider.class);

    private final SmppSessionManager.SmppConfig config;

    public SmppSmsProvider(String host, int port, String systemId, String password, String addressRange) {
        this.config = new SmppSessionManager.SmppConfig(host, port, systemId, password, addressRange);
    }

    @Override
    public SmsResult send(String to, String message, String from) {
        try {
            String msgId = SmppSessionManager.submit(config, to, message, from);
            return new SmsResult(msgId, msgId);
        } catch (Exception e) {
            log.error("SMPP send failed for {}: {}", to, e.getMessage());
            return new SmsResult(false, "SMPP error: " + e.getMessage());
        }
    }

    @Override
    public SmsProviderType getType() {
        return SmsProviderType.SMPP;
    }
}
