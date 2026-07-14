package com.twilio.twilio_project;

import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class SmppSessionManager {
    private static final Logger log = LoggerFactory.getLogger(SmppSessionManager.class);
    private static final ConcurrentHashMap<String, SMPPSession> sessions = new ConcurrentHashMap<>();

    public static class SmppConfig {
        public final String host;
        public final int port;
        public final String systemId;
        public final String password;
        public final String addressRange;

        public SmppConfig(String host, int port, String systemId, String password, String addressRange) {
            this.host = host;
            this.port = port;
            this.systemId = systemId;
            this.password = password;
            this.addressRange = addressRange;
        }

        String key() {
            return host + ":" + port + ":" + systemId;
        }
    }

    public static synchronized SMPPSession getSession(SmppConfig cfg) throws IOException {
        String key = cfg.key();
        SMPPSession session = sessions.get(key);
        if (session != null && session.getSessionState().isBound()) {
            return session;
        }
        if (session != null) {
            try { session.unbindAndClose(); } catch (Exception ignored) {}
        }
        session = new SMPPSession();
        session.setEnquireLinkTimer(30000);
        session.setTransactionTimer(10000);
        session.addSessionStateListener((newState, oldState, source) -> {
            if (newState.equals(SessionState.CLOSED)) {
                sessions.remove(key, source);
                log.warn("SMPP session {} closed, removed from pool", key);
            }
        });
        session.setMessageReceiverListener(new MessageReceiverListener() {
            @Override
            public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
                try {
                    byte esmClass = deliverSm.getEsmClass();
                    if (esmClass == 4) {
                        handleDeliveryReceipt(deliverSm);
                    } else {
                        handleInboundMessage(deliverSm);
                    }
                } catch (Exception e) {
                    log.error("Error processing DELIVER_SM: {}", e.getMessage());
                }
            }

            @Override
            public void onAcceptAlertNotification(AlertNotification alertNotification) {
                log.info("SMPP alert: {}", alertNotification);
            }

            @Override
            public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
                return null;
            }
        });
        session.connectAndBind(cfg.host, cfg.port,
                new BindParameter(BindType.BIND_TRX, cfg.systemId, cfg.password, "",
                        TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, cfg.addressRange != null ? cfg.addressRange : ""));
        sessions.put(key, session);
        log.info("SMPP session bound to {} ({})", key, session.getSessionState());
        return session;
    }

    private static void handleDeliveryReceipt(DeliverSm deliverSm) {
        try {
            String msgStr = new String(deliverSm.getShortMessage(), "UTF-8");
            org.jsmpp.bean.DeliveryReceipt receipt = new org.jsmpp.bean.DeliveryReceipt(msgStr);
            String providerRefId = receipt.getId();
            String status = receipt.getFinalStatus() != null ? receipt.getFinalStatus().name() : "DELIVRD";
            UserRepository.updateSmsStatusByProviderRefId(providerRefId, status);
            log.info("DLR for {}: {}", providerRefId, status);
        } catch (Exception e) {
            log.warn("Failed to parse DLR: {}", e.getMessage());
        }
    }

    private static void handleInboundMessage(DeliverSm deliverSm) {
        try {
            String from = deliverSm.getSourceAddr();
            String to = deliverSm.getDestAddress();
            String message = new String(deliverSm.getShortMessage(), "UTF-8");
            int userId = UserRepository.findUserIdByPhone(to);
            if (userId > 0) {
                UserRepository.saveInboundSms(userId, from, to, message);
                log.info("Inbound SMS for user {} from {}: {}", userId, from, message);
            } else {
                log.warn("No user found for inbound SMS to {}", to);
            }
        } catch (SQLException e) {
            log.warn("DB error storing inbound SMS: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to process inbound SMS: {}", e.getMessage());
        }
    }

    public static String submit(SmppConfig cfg, String to, String message, String from) throws IOException {
        try {
            SMPPSession session = getSession(cfg);
            String sourceAddr = from != null && !from.isEmpty() ? from : cfg.addressRange;

            SubmitSmResult result = session.submitShortMessage(
                    "CMT",
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN,
                    sourceAddr,
                    TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN,
                    to,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                    (byte) 0,
                    new GeneralDataCoding(),
                    (byte) 0,
                    message.getBytes()
            );
            return result.getMessageId();
        } catch (Exception e) {
            throw new IOException("SMPP submit failed: " + e.getMessage(), e);
        }
    }

    public static void closeAll() {
        sessions.forEach((key, session) -> {
            try { session.unbindAndClose(); } catch (Exception ignored) {}
        });
        sessions.clear();
    }
}