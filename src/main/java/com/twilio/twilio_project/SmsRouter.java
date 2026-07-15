package com.twilio.twilio_project; // SMS router — picks SMPP or Twilio provider per user config, sends, logs

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Routes outbound SMS through the user's configured provider (SMPP / Twilio / AUTO).
// Provider resolution: user's smsProvider column → EnvLoader fallback.
// AUTO mode tries SMPP first, falls back to Twilio on failure.
public class SmsRouter {
    private static final Logger log = LoggerFactory.getLogger(SmsRouter.class);
    private static final TwilioSmsProvider twilio = new TwilioSmsProvider();

    public static SmsResult send(String to, String message, int userId) {
        String provider = UserRepository.findSmsProvider(userId);
        if (provider == null) provider = "TWILIO";

        SmppConfig smpp = resolveSmppConfig(userId);
        boolean smppConfigured = smpp.host != null && !smpp.host.isEmpty()
                && smpp.systemId != null && !smpp.systemId.isEmpty();

        switch (provider.toUpperCase()) {
            case "SMPP":
                if (!smppConfigured) {
                    return new SmsResult(false, "SMPP selected but SMSC not configured");
                }
                return sendSmpp(smpp, to, message);

            // AUTO: prefer SMPP, fall back to Twilio if SMPP fails
            case "AUTO":
                if (smppConfigured) {
                    SmsResult result = sendSmpp(smpp, to, message);
                    if (result.isSuccess()) return result;
                    log.warn("SMPP fallback to Twilio for user {}: {}", userId, result.getError());
                }
                return sendTwilioWithUserCreds(to, message, userId);

            default:
                return sendTwilioWithUserCreds(to, message, userId);
        }
    }

    // Load SMPP config from user's DB fields, fall back to EnvLoader (global defaults).
    private static SmppConfig resolveSmppConfig(int userId) {
        // host/port — EnvLoader is authoritative (profile-aware: localhost vs smscsim),
        // DB overrides for per-user SMSC
        String host = EnvLoader.get("SMPP_HOST");
        String port = EnvLoader.get("SMPP_PORT");
        String dbHost = UserRepository.findSmppConfig(userId, "smpp_host");
        String dbPort = UserRepository.findSmppConfig(userId, "smpp_port");
        if (dbHost != null && !dbHost.isEmpty()) host = dbHost;
        if (dbPort != null && !dbPort.isEmpty()) port = dbPort;

        // sid/pass/addr — per-user credentials from DB, fallback to EnvLoader
        String sid = UserRepository.findSmppConfig(userId, "smpp_system_id");
        String pass = UserRepository.findSmppConfig(userId, "smpp_password");
        String addr = UserRepository.findSmppConfig(userId, "smpp_address_range");
        if (sid == null || sid.isEmpty()) sid = EnvLoader.get("SMPP_SYSTEM_ID");
        if (pass == null || pass.isEmpty()) pass = EnvLoader.get("SMPP_PASSWORD");
        if (addr == null || addr.isEmpty()) addr = EnvLoader.get("SMPP_ADDRESS_RANGE");

        return new SmppConfig(host, parseInt(port, 2776), sid, pass, addr);
    }

    public static class SmppConfig {
        public final String host;
        public final int port;
        public final String systemId;
        public final String password;
        public final String addressRange;

        SmppConfig(String host, int port, String systemId, String password, String addressRange) {
            this.host = host;
            this.port = port;
            this.systemId = systemId;
            this.password = password;
            this.addressRange = addressRange;
        }
    }

    private static SmsResult sendSmpp(SmppConfig cfg, String to, String message) {
        SmppSmsProvider smpp = new SmppSmsProvider(cfg.host, cfg.port, cfg.systemId, cfg.password, cfg.addressRange);
        return smpp.send(to, message, cfg.addressRange);
    }

    // Initialize Twilio with user's credentials, then delegate to TwilioSmsProvider.
    private static SmsResult sendTwilioWithUserCreds(String to, String message, int userId) {
        try {
            CustomerTwilioConfig cfg = UserRepository.findTwilioConfigByUserId(userId);
            if (cfg == null || !cfg.isComplete()) {
                return new SmsResult(false, "Twilio credentials not configured for this user");
            }
            Twilio.init(cfg.getAccountSid(), cfg.getAuthToken());
            return twilio.send(to, message, PhoneUtil.normalize(cfg.getSenderId()));
        } catch (Exception e) {
            return new SmsResult(false, "Twilio error: " + e.getMessage());
        }
    }

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }
}
