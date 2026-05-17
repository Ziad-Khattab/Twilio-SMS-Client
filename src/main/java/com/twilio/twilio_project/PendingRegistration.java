package com.twilio.twilio_project;

import java.io.Serializable;
import java.sql.Date;

public class PendingRegistration implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private String fullName;
    private Date birthday;
    private String msisdn;
    private String job;
    private String email;
    private String address;
    private String twilioAccountSid;
    private String twilioAuthToken;
    private String twilioSenderId;
    private String verificationCode;
    private long verificationExpiresAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTwilioAccountSid() {
        return twilioAccountSid;
    }

    public void setTwilioAccountSid(String twilioAccountSid) {
        this.twilioAccountSid = twilioAccountSid;
    }

    public String getTwilioAuthToken() {
        return twilioAuthToken;
    }

    public void setTwilioAuthToken(String twilioAuthToken) {
        this.twilioAuthToken = twilioAuthToken;
    }

    public String getTwilioSenderId() {
        return twilioSenderId;
    }

    public void setTwilioSenderId(String twilioSenderId) {
        this.twilioSenderId = twilioSenderId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public long getVerificationExpiresAt() {
        return verificationExpiresAt;
    }

    public void setVerificationExpiresAt(long verificationExpiresAt) {
        this.verificationExpiresAt = verificationExpiresAt;
    }

    public boolean isVerificationExpired() {
        return System.currentTimeMillis() > verificationExpiresAt;
    }
}
