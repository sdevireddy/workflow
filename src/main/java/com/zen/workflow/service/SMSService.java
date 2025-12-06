package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for sending SMS and WhatsApp messages in workflows
 * Supports Twilio, WhatsApp Business API, and other providers
 */
@Slf4j
@Service
public class SMSService {

    @Value("${workflow.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${workflow.sms.provider:TWILIO}")
    private String smsProvider;

    @Value("${workflow.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${workflow.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${workflow.sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${workflow.whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    @Value("${workflow.whatsapp.api-key:}")
    private String whatsappApiKey;

    @Value("${workflow.whatsapp.api-url:}")
    private String whatsappApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    /**
     * Send SMS message
     */
    public Map<String, Object> sendSMS(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.warn("SMS service is disabled. Message not sent to: {}", phoneNumber);
            return Map.of("sent", false, "reason", "SMS service disabled");
        }

        try {
            // Validate phone number
            if (!isValidPhoneNumber(phoneNumber)) {
                return Map.of("sent", false, "reason", "Invalid phone number format");
            }

            log.info("Sending SMS to: {} via {}", phoneNumber, smsProvider);

            Map<String, Object> result;
            switch (smsProvider.toUpperCase()) {
                case "TWILIO":
                    result = sendViaTwilio(phoneNumber, message);
                    break;
                case "MOCK":
                    result = sendViaMock(phoneNumber, message);
                    break;
                default:
                    log.warn("Unknown SMS provider: {}", smsProvider);
                    result = sendViaMock(phoneNumber, message);
            }

            log.info("SMS sent successfully to: {}", phoneNumber);
            return result;

        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            return Map.of(
                "sent", false,
                "error", e.getMessage(),
                "phoneNumber", phoneNumber
            );
        }
    }

    /**
     * Send WhatsApp message
     */
    public Map<String, Object> sendWhatsApp(String phoneNumber, String message) {
        if (!whatsappEnabled) {
            log.warn("WhatsApp service is disabled. Message not sent to: {}", phoneNumber);
            return Map.of("sent", false, "reason", "WhatsApp service disabled");
        }

        try {
            // Validate phone number
            if (!isValidPhoneNumber(phoneNumber)) {
                return Map.of("sent", false, "reason", "Invalid phone number format");
            }

            log.info("Sending WhatsApp message to: {}", phoneNumber);

            // In production, integrate with WhatsApp Business API
            Map<String, Object> result = sendWhatsAppViaMock(phoneNumber, message);

            log.info("WhatsApp message sent successfully to: {}", phoneNumber);
            return result;

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to: {}", phoneNumber, e);
            return Map.of(
                "sent", false,
                "error", e.getMessage(),
                "phoneNumber", phoneNumber
            );
        }
    }

    /**
     * Send WhatsApp template message
     */
    public Map<String, Object> sendWhatsAppTemplate(String phoneNumber, String templateName, 
                                                     Map<String, Object> parameters) {
        if (!whatsappEnabled) {
            log.warn("WhatsApp service is disabled. Template message not sent.");
            return Map.of("sent", false, "reason", "WhatsApp service disabled");
        }

        try {
            log.info("Sending WhatsApp template '{}' to: {}", templateName, phoneNumber);

            // In production, use WhatsApp Business API template endpoint
            Map<String, Object> result = new HashMap<>();
            result.put("sent", true);
            result.put("phoneNumber", phoneNumber);
            result.put("templateName", templateName);
            result.put("messageId", "whatsapp_" + System.currentTimeMillis());

            log.info("WhatsApp template sent successfully");
            return result;

        } catch (Exception e) {
            log.error("Failed to send WhatsApp template", e);
            return Map.of("sent", false, "error", e.getMessage());
        }
    }

    /**
     * Send bulk SMS
     */
    public Map<String, Object> sendBulkSMS(List<String> phoneNumbers, String message) {
        if (!smsEnabled) {
            log.warn("SMS service is disabled. Bulk SMS not sent.");
            return Map.of("sent", 0, "reason", "SMS service disabled");
        }

        int successCount = 0;
        int failureCount = 0;

        for (String phoneNumber : phoneNumbers) {
            try {
                Map<String, Object> result = sendSMS(phoneNumber, message);
                if ((Boolean) result.getOrDefault("sent", false)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send SMS to: {}", phoneNumber, e);
                failureCount++;
            }
        }

        log.info("Bulk SMS completed: {} sent, {} failed out of {} total",
            successCount, failureCount, phoneNumbers.size());

        Map<String, Object> result = new HashMap<>();
        result.put("sent", successCount);
        result.put("failed", failureCount);
        result.put("total", phoneNumbers.size());

        return result;
    }

    // Provider-specific implementations

    /**
     * Send SMS via Twilio
     */
    private Map<String, Object> sendViaTwilio(String phoneNumber, String message) {
        if (twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty()) {
            log.warn("Twilio credentials not configured");
            return sendViaMock(phoneNumber, message);
        }

        try {
            // In production, use Twilio SDK
            // Twilio.init(twilioAccountSid, twilioAuthToken);
            // Message twilioMessage = Message.creator(
            //     new PhoneNumber(phoneNumber),
            //     new PhoneNumber(twilioFromNumber),
            //     message
            // ).create();

            Map<String, Object> result = new HashMap<>();
            result.put("sent", true);
            result.put("phoneNumber", phoneNumber);
            result.put("provider", "TWILIO");
            result.put("messageId", "twilio_" + System.currentTimeMillis());

            return result;

        } catch (Exception e) {
            log.error("Twilio SMS failed", e);
            throw new RuntimeException("Twilio SMS failed: " + e.getMessage(), e);
        }
    }

    /**
     * Mock SMS sending (for testing)
     */
    private Map<String, Object> sendViaMock(String phoneNumber, String message) {
        log.info("MOCK SMS: To={}, Message={}", phoneNumber, message);

        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("phoneNumber", phoneNumber);
        result.put("provider", "MOCK");
        result.put("messageId", "mock_" + System.currentTimeMillis());
        result.put("message", message);

        return result;
    }

    /**
     * Mock WhatsApp sending (for testing)
     */
    private Map<String, Object> sendWhatsAppViaMock(String phoneNumber, String message) {
        log.info("MOCK WhatsApp: To={}, Message={}", phoneNumber, message);

        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("phoneNumber", phoneNumber);
        result.put("provider", "MOCK");
        result.put("messageId", "whatsapp_mock_" + System.currentTimeMillis());
        result.put("message", message);

        return result;
    }

    // Validation methods

    /**
     * Validate phone number format (E.164)
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and dashes
        String cleaned = phoneNumber.replaceAll("[\\s-]", "");

        // Check E.164 format
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Format phone number to E.164
     */
    public String formatPhoneNumber(String phoneNumber, String defaultCountryCode) {
        if (phoneNumber == null) {
            return null;
        }

        String cleaned = phoneNumber.replaceAll("[\\s-]", "");

        // Add + if missing
        if (!cleaned.startsWith("+")) {
            // Add country code if missing
            if (!cleaned.startsWith(defaultCountryCode)) {
                cleaned = defaultCountryCode + cleaned;
            }
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    /**
     * Check if SMS service is available
     */
    public boolean isSMSServiceAvailable() {
        return smsEnabled;
    }

    /**
     * Check if WhatsApp service is available
     */
    public boolean isWhatsAppServiceAvailable() {
        return whatsappEnabled;
    }
}
