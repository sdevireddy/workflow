package com.zen.workflow.handler;

import com.zen.workflow.engine.VariableResolver;
import com.zen.workflow.model.ExecutionContext;
import com.zen.workflow.model.ExecutionResult;
import com.zen.workflow.model.NodeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all communication operations:
 * - send_email, send_template_email, send_bulk_email
 * - send_sms, send_whatsapp
 * - send_notification, internal_notification, push_notification
 * - post_to_chat, slack_message
 */
@Slf4j
@Component
public class EmailHandler implements NodeHandler {

    @Autowired
    private VariableResolver variableResolver;
    
    @Autowired
    private com.zen.workflow.service.EmailService emailService;
    
    @Autowired
    private com.zen.workflow.service.SMSService smsService;
    
    @Autowired
    private com.zen.workflow.service.NotificationService notificationService;
    
    @Autowired
    private com.zen.workflow.service.ChatService chatService;

    @Override
    public ExecutionResult execute(NodeConfig config, ExecutionContext context) {
        String subtype = config.getSubtype();
        log.info("Executing Communication: {}", subtype);

        try {
            switch (subtype) {
                case "send_email":
                    return handleSendEmail(config, context);
                case "send_template_email":
                    return handleSendTemplateEmail(config, context);
                case "send_bulk_email":
                    return handleSendBulkEmail(config, context);
                case "send_sms":
                    return handleSendSMS(config, context);
                case "send_whatsapp":
                    return handleSendWhatsApp(config, context);
                case "send_notification":
                case "internal_notification":
                case "push_notification":
                    return handleSendNotification(config, context);
                case "post_to_chat":
                case "slack_message":
                    return handlePostToChat(config, context);
                default:
                    return ExecutionResult.failed("Unknown communication type: " + subtype);
            }
        } catch (Exception e) {
            log.error("Communication failed", e);
            return ExecutionResult.failed(e.getMessage());
        }
    }

    private ExecutionResult handleSendEmail(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String to = variableResolver.resolve((String) nodeConfig.get("to"), context);
        String subject = variableResolver.resolve((String) nodeConfig.get("subject"), context);
        String body = variableResolver.resolve((String) nodeConfig.get("body"), context);
        String cc = variableResolver.resolve((String) nodeConfig.get("cc"), context);
        String bcc = variableResolver.resolve((String) nodeConfig.get("bcc"), context);
        Boolean isHtml = (Boolean) nodeConfig.getOrDefault("isHtml", false);
        
        log.info("Sending email to: {}, subject: {}", to, subject);
        
        try {
            // Validate email
            if (!emailService.isValidEmail(to)) {
                return ExecutionResult.failed("Invalid email address: " + to);
            }
            
            // Check if email service is available
            if (!emailService.isEmailServiceAvailable()) {
                log.warn("Email service not available. Email will not be sent.");
                Map<String, Object> output = new HashMap<>();
                output.put("emailSent", false);
                output.put("reason", "Email service not configured");
                output.put("to", to);
                return ExecutionResult.success(output);
            }
            
            // Send email
            if (cc != null || bcc != null) {
                emailService.sendEmail(to, subject, body, cc, bcc, isHtml);
            } else {
                emailService.sendEmail(to, subject, body, isHtml);
            }
            
            Map<String, Object> output = new HashMap<>();
            output.put("emailSent", true);
            output.put("to", to);
            output.put("subject", subject);
            output.put("isHtml", isHtml);
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to send email", e);
            return ExecutionResult.failed("Email sending failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleSendTemplateEmail(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String to = variableResolver.resolve((String) nodeConfig.get("to"), context);
        String subject = variableResolver.resolve((String) nodeConfig.get("subject"), context);
        String template = variableResolver.resolve((String) nodeConfig.get("template"), context);
        Map<String, Object> variables = (Map<String, Object>) nodeConfig.get("variables");
        
        // Resolve variables
        if (variables != null) {
            variables = variableResolver.resolveMap(variables, context);
        } else {
            variables = new HashMap<>();
        }
        
        log.info("Sending template email to: {}", to);
        
        try {
            // Validate email
            if (!emailService.isValidEmail(to)) {
                return ExecutionResult.failed("Invalid email address: " + to);
            }
            
            // Check if email service is available
            if (!emailService.isEmailServiceAvailable()) {
                log.warn("Email service not available. Template email will not be sent.");
                Map<String, Object> output = new HashMap<>();
                output.put("emailSent", false);
                output.put("reason", "Email service not configured");
                return ExecutionResult.success(output);
            }
            
            // Send template email
            emailService.sendTemplateEmail(to, subject, template, variables);
            
            Map<String, Object> output = new HashMap<>();
            output.put("emailSent", true);
            output.put("to", to);
            output.put("subject", subject);
            output.put("variablesUsed", variables.keySet());
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to send template email", e);
            return ExecutionResult.failed("Template email sending failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleSendBulkEmail(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        List<String> recipients = (List<String>) nodeConfig.get("recipients");
        String subject = variableResolver.resolve((String) nodeConfig.get("subject"), context);
        String body = variableResolver.resolve((String) nodeConfig.get("body"), context);
        Boolean isHtml = (Boolean) nodeConfig.getOrDefault("isHtml", false);
        
        log.info("Sending bulk email to {} recipients", recipients.size());
        
        try {
            // Check if email service is available
            if (!emailService.isEmailServiceAvailable()) {
                log.warn("Email service not available. Bulk email will not be sent.");
                Map<String, Object> output = new HashMap<>();
                output.put("emailsSent", 0);
                output.put("reason", "Email service not configured");
                return ExecutionResult.success(output);
            }
            
            // Send bulk email
            emailService.sendBulkEmail(recipients, subject, body, isHtml);
            
            Map<String, Object> output = new HashMap<>();
            output.put("emailsSent", recipients.size());
            output.put("subject", subject);
            output.put("isHtml", isHtml);
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to send bulk email", e);
            return ExecutionResult.failed("Bulk email sending failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleSendSMS(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String phoneNumber = variableResolver.resolve((String) nodeConfig.get("phoneNumber"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        
        log.info("Sending SMS to: {}", phoneNumber);
        
        try {
            // Validate phone number
            if (!smsService.isValidPhoneNumber(phoneNumber)) {
                return ExecutionResult.failed("Invalid phone number format: " + phoneNumber);
            }
            
            // Check if SMS service is available
            if (!smsService.isSMSServiceAvailable()) {
                log.warn("SMS service not available.");
                return ExecutionResult.success(Map.of(
                    "smsSent", false,
                    "reason", "SMS service not configured"
                ));
            }
            
            // Send SMS
            Map<String, Object> result = smsService.sendSMS(phoneNumber, message);
            
            // Store in context
            context.setVariable("smsResult", result);
            context.setVariable("smsMessageId", result.get("messageId"));
            
            Map<String, Object> output = new HashMap<>();
            output.put("smsSent", result.get("sent"));
            output.put("phoneNumber", phoneNumber);
            output.put("messageId", result.get("messageId"));
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to send SMS", e);
            return ExecutionResult.failed("SMS sending failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleSendWhatsApp(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        
        String phoneNumber = variableResolver.resolve((String) nodeConfig.get("phoneNumber"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        String templateName = (String) nodeConfig.get("templateName");
        Map<String, Object> parameters = (Map<String, Object>) nodeConfig.get("parameters");
        
        log.info("Sending WhatsApp message to: {}", phoneNumber);
        
        try {
            // Validate phone number
            if (!smsService.isValidPhoneNumber(phoneNumber)) {
                return ExecutionResult.failed("Invalid phone number format: " + phoneNumber);
            }
            
            // Check if WhatsApp service is available
            if (!smsService.isWhatsAppServiceAvailable()) {
                log.warn("WhatsApp service not available.");
                return ExecutionResult.success(Map.of(
                    "whatsappSent", false,
                    "reason", "WhatsApp service not configured"
                ));
            }
            
            // Send WhatsApp message
            Map<String, Object> result;
            if (templateName != null) {
                // Send template message
                if (parameters != null) {
                    parameters = variableResolver.resolveMap(parameters, context);
                }
                result = smsService.sendWhatsAppTemplate(phoneNumber, templateName, parameters);
            } else {
                // Send regular message
                result = smsService.sendWhatsApp(phoneNumber, message);
            }
            
            // Store in context
            context.setVariable("whatsappResult", result);
            context.setVariable("whatsappMessageId", result.get("messageId"));
            
            Map<String, Object> output = new HashMap<>();
            output.put("whatsappSent", result.get("sent"));
            output.put("phoneNumber", phoneNumber);
            output.put("messageId", result.get("messageId"));
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message", e);
            return ExecutionResult.failed("WhatsApp sending failed: " + e.getMessage());
        }
    }

    private ExecutionResult handleSendNotification(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        String userId = variableResolver.resolve((String) nodeConfig.get("userId"), context);
        String title = variableResolver.resolve((String) nodeConfig.get("title"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        String type = (String) nodeConfig.getOrDefault("type", "INFO");
        Map<String, Object> data = (Map<String, Object>) nodeConfig.get("data");
        
        log.info("Sending {} notification to user: {}", subtype, userId);
        
        try {
            // Check if notification service is available
            if (!notificationService.isNotificationServiceAvailable()) {
                log.warn("Notification service not available.");
                return ExecutionResult.success(Map.of(
                    "notificationSent", false,
                    "reason", "Notification service not configured"
                ));
            }
            
            // Resolve data variables
            if (data != null) {
                data = variableResolver.resolveMap(data, context);
            }
            
            Map<String, Object> result;
            
            switch (subtype) {
                case "push_notification":
                    // Send push notification
                    if (!notificationService.isPushServiceAvailable()) {
                        return ExecutionResult.success(Map.of(
                            "notificationSent", false,
                            "reason", "Push notification service not configured"
                        ));
                    }
                    result = notificationService.sendPushNotification(
                        Long.parseLong(userId), title, message, data
                    );
                    break;
                    
                case "internal_notification":
                case "send_notification":
                default:
                    // Send internal notification
                    result = notificationService.sendNotification(
                        Long.parseLong(userId), title, message, type, data
                    );
                    break;
            }
            
            // Store in context
            context.setVariable("notificationResult", result);
            context.setVariable("notificationId", result.get("id"));
            
            Map<String, Object> output = new HashMap<>();
            output.put("notificationSent", true);
            output.put("userId", userId);
            output.put("notificationId", result.get("id"));
            output.put("type", type);
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            return ExecutionResult.failed("Notification sending failed: " + e.getMessage());
        }
    }

    private ExecutionResult handlePostToChat(NodeConfig config, ExecutionContext context) {
        Map<String, Object> nodeConfig = config.getConfig();
        String subtype = config.getSubtype();
        
        String channel = variableResolver.resolve((String) nodeConfig.get("channel"), context);
        String message = variableResolver.resolve((String) nodeConfig.get("message"), context);
        String platform = (String) nodeConfig.getOrDefault("platform", "SLACK");
        Map<String, Object> options = (Map<String, Object>) nodeConfig.get("options");
        
        log.info("Posting to {} channel: {}", platform, channel);
        
        try {
            if (!chatService.isChatServiceAvailable()) {
                log.warn("Chat service not available.");
                return ExecutionResult.success(Map.of(
                    "messageSent", false,
                    "reason", "Chat service not configured"
                ));
            }
            
            // Resolve options
            if (options != null) {
                options = variableResolver.resolveMap(options, context);
            }
            
            Map<String, Object> result;
            
            if ("slack_message".equals(subtype)) {
                // Slack-specific message
                if (!chatService.isSlackServiceAvailable()) {
                    return ExecutionResult.success(Map.of(
                        "messageSent", false,
                        "reason", "Slack service not configured"
                    ));
                }
                result = chatService.sendSlackMessage(channel, message, options);
            } else {
                // Generic chat message
                result = chatService.postToChat(platform, channel, message, options);
            }
            
            // Store in context
            context.setVariable("chatResult", result);
            context.setVariable("chatMessageId", result.get("messageId"));
            
            Map<String, Object> output = new HashMap<>();
            output.put("messageSent", result.get("sent"));
            output.put("channel", channel);
            output.put("platform", platform);
            output.put("messageId", result.get("messageId"));
            
            return ExecutionResult.success(output);
            
        } catch (Exception e) {
            log.error("Failed to post to chat", e);
            return ExecutionResult.failed("Chat message failed: " + e.getMessage());
        }
    }
}
