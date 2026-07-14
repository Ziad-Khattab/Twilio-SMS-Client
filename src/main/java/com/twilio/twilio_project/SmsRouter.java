package com.twilio.twilio_project;

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes SMS sends based on per-user provider config.
 * Provider options:
 *   TWILIO — always use Twilio
 *   SMPP   — always use SMPP (fails if SMSC not configured)
 *   AUTO   — try SMPP first, fallback to Twilio on failure
 */
public class SmsRouter {
    private static final Logger log = LoggerFactory.getLogger(SmsRouter.class);
    private static final TwilioSmsProvider twilio = new TwilioSmsProvider();

    public static SmsResult send(String to, String message, int userId) {
        String provider = UserRepository.findSmsProvider(userId);
        if (provider == null) provider = "TWILIO";

        String smppHost = UserRepository.findSmppConfig(userId, "smpp_host");
        int smppPort = parseInt(UserRepository.findSmppConfig(userId, "smpp_port"), 2776);
        String smppSystemId = UserRepository.findSmppConfig(userId, "smpp_system_id");
        String smppPassword = UserRepository.findSmppConfig(userId, "smpp_password");
        String smppAddrRange = UserRepository.findSmppConfig(userId, "smpp_address_range");

        boolean smppConfigured = smppHost != null && !smppHost.isEmpty()
                && smppSystemId != null && !smppSystemId.isEmpty();

        SmsResult result;

        switch (provider.toUpperCase()) {
            case "SMPP":
                if (!smppConfigured) {
                    return new SmsResult(false, "SMPP selected but SMSC not configured");
                }
                result = sendSmpp(smppHost, smppPort, smppSystemId, smppPassword, smppAddrRange, to, message);
                if (!result.isSuccess()) {
                    log.warn("SMPP send failed for user {}: {}", userId, result.getError());
                }
                return result;

            case "AUTO":
                if (smppConfigured) {
                    result = sendSmpp(smppHost, smppPort, smppSystemId, smppPassword, smppAddrRange, to, message);
                    if (result.isSuccess()) return result;
                    log.warn("SMPP fallback to Twilio for user {}: {}", userId, result.getError());
                }
                return sendTwilioWithUserCreds(to, message, userId);

            default: // TWILIO
                return sendTwilioWithUserCreds(to, message, userId);
        }
    }

    private static SmsResult sendSmpp(String host, int port, String sid, String pass, String addrRange,
                                       String to, String message) {
        SmppSmsProvider smpp = new SmppSmsProvider(host, port, sid, pass, addrRange);
        return smpp.send(to, message, addrRange);
    }

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
