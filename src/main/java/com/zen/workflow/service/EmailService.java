package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

/**
 * Service for sending emails in workflows
 * Supports simple emails, HTML emails, templates, and bulk sending
 */
@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@example.com}")
    private String defaultFromEmail;

    @Value("${workflow.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Send simple text email
     */
    public void sendEmail(String to, String subject, String body) {
        sendEmail(to, subject, body, false);
    }

    /**
     * Send email (text or HTML)
     */
    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Email not sent to: {}", to);
            return;
        }

        if (mailSender == null) {
            log.error("JavaMailSender not configured. Cannot send email.");
            throw new RuntimeException("Email service not configured");
        }

        try {
            if (isHtml) {
                sendHtmlEmail(to, subject, body);
            } else {
                sendTextEmail(to, subject, body);
            }
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send simple text email
     */
    private void sendTextEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultFromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }

    /**
     * Send HTML email
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(defaultFromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        
        mailSender.send(message);
    }

    /**
     * Send email with CC and BCC
     */
    public void sendEmail(String to, String subject, String body, String cc, String bcc, boolean isHtml) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Email not sent to: {}", to);
            return;
        }

        if (mailSender == null) {
            log.error("JavaMailSender not configured. Cannot send email.");
            throw new RuntimeException("Email service not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(defaultFromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml);
            
            if (cc != null && !cc.trim().isEmpty()) {
                helper.setCc(cc.split(","));
            }
            
            if (bcc != null && !bcc.trim().isEmpty()) {
                helper.setBcc(bcc.split(","));
            }
            
            mailSender.send(message);
            log.info("Email sent successfully to: {} (CC: {}, BCC: {})", to, cc, bcc);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send template email with variable substitution
     */
    public void sendTemplateEmail(String to, String subject, String template, Map<String, Object> variables) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Template email not sent to: {}", to);
            return;
        }

        try {
            // Replace variables in template
            String processedBody = processTemplate(template, variables);
            
            // Determine if HTML based on template content
            boolean isHtml = template.contains("<html") || template.contains("<body") || template.contains("<div");
            
            sendEmail(to, subject, processedBody, isHtml);
            log.info("Template email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send template email to: {}", to, e);
            throw new RuntimeException("Failed to send template email: " + e.getMessage(), e);
        }
    }

    /**
     * Send bulk emails
     */
    public void sendBulkEmail(List<String> recipients, String subject, String body, boolean isHtml) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Bulk email not sent to {} recipients", recipients.size());
            return;
        }

        if (mailSender == null) {
            log.error("JavaMailSender not configured. Cannot send bulk email.");
            throw new RuntimeException("Email service not configured");
        }

        int successCount = 0;
        int failureCount = 0;

        for (String recipient : recipients) {
            try {
                sendEmail(recipient, subject, body, isHtml);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send bulk email to: {}", recipient, e);
                failureCount++;
            }
        }

        log.info("Bulk email completed: {} sent, {} failed out of {} total", 
            successCount, failureCount, recipients.size());

        if (failureCount > 0) {
            throw new RuntimeException(String.format(
                "Bulk email partially failed: %d sent, %d failed", successCount, failureCount));
        }
    }

    /**
     * Send bulk template emails with per-recipient variables
     */
    public void sendBulkTemplateEmail(List<Map<String, Object>> recipientData, 
                                      String subject, String template) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Bulk template email not sent to {} recipients", 
                recipientData.size());
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (Map<String, Object> data : recipientData) {
            try {
                String to = (String) data.get("email");
                if (to == null || to.trim().isEmpty()) {
                    log.warn("Skipping recipient with no email address");
                    failureCount++;
                    continue;
                }

                // Process subject with variables
                String processedSubject = processTemplate(subject, data);
                
                // Send template email
                sendTemplateEmail(to, processedSubject, template, data);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send bulk template email", e);
                failureCount++;
            }
        }

        log.info("Bulk template email completed: {} sent, {} failed out of {} total", 
            successCount, failureCount, recipientData.size());

        if (failureCount > 0) {
            throw new RuntimeException(String.format(
                "Bulk template email partially failed: %d sent, %d failed", successCount, failureCount));
        }
    }

    /**
     * Process template by replacing variables
     */
    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        String processed = template;
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processed = processed.replace(placeholder, value);
        }

        return processed;
    }

    /**
     * Validate email address format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Check if email service is configured and enabled
     */
    public boolean isEmailServiceAvailable() {
        return emailEnabled && mailSender != null;
    }
}
