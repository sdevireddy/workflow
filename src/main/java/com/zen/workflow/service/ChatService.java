package com.zen.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending messages to chat platforms (Slack, Teams, etc.)
 */
@Slf4j
@Service
public class ChatService {

    @Value("${workflow.chat.enabled:false}")
    private boolean chatEnabled;

    @Value("${workflow.slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${workflow.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${workflow.slack.bot-token:}")
    private String slackBotToken;

    @Value("${workflow.teams.enabled:false}")
    private boolean teamsEnabled;

    @Value("${workflow.teams.webhook-url:}")
    private String teamsWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send message to Slack
     */
    public Map<String, Object> sendSlackMessage(String channel, String message, 
                                                Map<String, Object> attachments) {
        if (!slackEnabled) {
            log.warn("Slack service is disabled.");
            return Map.of("sent", false, "reason", "Slack service disabled");
        }

        try {
            log.info("Sending Slack message to channel: {}", channel);

            // Build Slack message payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", channel);
            payload.put("text", message);
            
            if (attachments != null && !attachments.isEmpty()) {
                payload.put("attachments", attachments);
            }

            // Send to Slack
            Map<String, Object> result;
            if (slackWebhookUrl != null && !slackWebhookUrl.isEmpty()) {
                result = sendViaWebhook(slackWebhookUrl, payload);
            } else if (slackBotToken != null && !slackBotToken.isEmpty()) {
                result = sendViaSlackAPI(payload);
            } else {
                result = sendSlackMock(channel, message);
            }

            log.info("Slack message sent successfully");
            return result;

        } catch (Exception e) {
            log.error("Failed to send Slack message", e);
            return Map.of(
                "sent", false,
                "error", e.getMessage(),
                "channel", channel
            );
        }
    }

    /**
     * Send message to Microsoft Teams
     */
    public Map<String, Object> sendTeamsMessage(String channel, String message,
                                                Map<String, Object> card) {
        if (!teamsEnabled) {
            log.warn("Teams service is disabled.");
            return Map.of("sent", false, "reason", "Teams service disabled");
        }

        try {
            log.info("Sending Teams message to channel: {}", channel);

            // Build Teams message payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("@type", "MessageCard");
            payload.put("@context", "https://schema.org/extensions");
            payload.put("text", message);
            
            if (card != null && !card.isEmpty()) {
                payload.putAll(card);
            }

            // Send to Teams
            Map<String, Object> result;
            if (teamsWebhookUrl != null && !teamsWebhookUrl.isEmpty()) {
                result = sendViaWebhook(teamsWebhookUrl, payload);
            } else {
                result = sendTeamsMock(channel, message);
            }

            log.info("Teams message sent successfully");
            return result;

        } catch (Exception e) {
            log.error("Failed to send Teams message", e);
            return Map.of(
                "sent", false,
                "error", e.getMessage(),
                "channel", channel
            );
        }
    }

    /**
     * Post to generic chat platform
     */
    public Map<String, Object> postToChat(String platform, String channel, String message,
                                          Map<String, Object> options) {
        if (!chatEnabled) {
            log.warn("Chat service is disabled.");
            return Map.of("sent", false, "reason", "Chat service disabled");
        }

        try {
            log.info("Posting to {} channel: {}", platform, channel);

            Map<String, Object> result;
            switch (platform.toUpperCase()) {
                case "SLACK":
                    result = sendSlackMessage(channel, message, options);
                    break;
                case "TEAMS":
                case "MICROSOFT_TEAMS":
                    result = sendTeamsMessage(channel, message, options);
                    break;
                default:
                    result = postToChatMock(platform, channel, message);
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to post to chat", e);
            return Map.of(
                "sent", false,
                "error", e.getMessage(),
                "platform", platform
            );
        }
    }

    /**
     * Send Slack message with rich formatting
     */
    public Map<String, Object> sendSlackRichMessage(String channel, String title, String text,
                                                     String color, Map<String, Object> fields) {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("title", title);
        attachment.put("text", text);
        attachment.put("color", color != null ? color : "good");
        
        if (fields != null) {
            attachment.put("fields", fields);
        }

        return sendSlackMessage(channel, "", Map.of("attachments", attachment));
    }

    /**
     * Send Teams adaptive card
     */
    public Map<String, Object> sendTeamsAdaptiveCard(String channel, String title,
                                                      String text, Map<String, Object> actions) {
        Map<String, Object> card = new HashMap<>();
        card.put("title", title);
        card.put("text", text);
        
        if (actions != null) {
            card.put("potentialAction", actions);
        }

        return sendTeamsMessage(channel, "", card);
    }

    // Provider-specific implementations

    /**
     * Send via webhook URL
     */
    private Map<String, Object> sendViaWebhook(String webhookUrl, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl,
                request,
                String.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("sent", true);
            result.put("statusCode", response.getStatusCodeValue());
            result.put("messageId", "webhook_" + System.currentTimeMillis());

            return result;

        } catch (Exception e) {
            log.error("Webhook send failed", e);
            throw new RuntimeException("Webhook send failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send via Slack API
     */
    private Map<String, Object> sendViaSlackAPI(Map<String, Object> payload) {
        try {
            String apiUrl = "https://slack.com/api/chat.postMessage";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(slackBotToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl,
                request,
                Map.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("sent", true);
            result.put("response", response.getBody());
            result.put("messageId", "slack_" + System.currentTimeMillis());

            return result;

        } catch (Exception e) {
            log.error("Slack API send failed", e);
            throw new RuntimeException("Slack API send failed: " + e.getMessage(), e);
        }
    }

    /**
     * Mock Slack sending (for testing)
     */
    private Map<String, Object> sendSlackMock(String channel, String message) {
        log.info("MOCK Slack: Channel={}, Message={}", channel, message);

        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("channel", channel);
        result.put("provider", "SLACK_MOCK");
        result.put("messageId", "slack_mock_" + System.currentTimeMillis());
        result.put("message", message);

        return result;
    }

    /**
     * Mock Teams sending (for testing)
     */
    private Map<String, Object> sendTeamsMock(String channel, String message) {
        log.info("MOCK Teams: Channel={}, Message={}", channel, message);

        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("channel", channel);
        result.put("provider", "TEAMS_MOCK");
        result.put("messageId", "teams_mock_" + System.currentTimeMillis());
        result.put("message", message);

        return result;
    }

    /**
     * Mock generic chat sending (for testing)
     */
    private Map<String, Object> postToChatMock(String platform, String channel, String message) {
        log.info("MOCK Chat: Platform={}, Channel={}, Message={}", platform, channel, message);

        Map<String, Object> result = new HashMap<>();
        result.put("sent", true);
        result.put("platform", platform);
        result.put("channel", channel);
        result.put("provider", "CHAT_MOCK");
        result.put("messageId", "chat_mock_" + System.currentTimeMillis());
        result.put("message", message);

        return result;
    }

    // Helper methods

    public boolean isChatServiceAvailable() {
        return chatEnabled;
    }

    public boolean isSlackServiceAvailable() {
        return slackEnabled;
    }

    public boolean isTeamsServiceAvailable() {
        return teamsEnabled;
    }
}
