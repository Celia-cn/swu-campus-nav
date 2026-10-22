package com.swu.gps.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swu.gps.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
public class DeepSeekClient {

    private final RestClient restClient;
    private final String model;
    private final boolean configured;
    private final int maxTokens;
    private final double temperature;
    private final int connectTimeoutSec;
    private final int readTimeoutSec;

    public DeepSeekClient(@Value("${app.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${app.deepseek.api-key:}") String apiKey,
                          @Value("${app.deepseek.model:deepseek-chat}") String model,
                          @Value("${app.deepseek.max-tokens:4096}") int maxTokens,
                          @Value("${app.deepseek.temperature:0.6}") double temperature,
                          @Value("${app.deepseek.connect-timeout-sec:15}") int connectTimeoutSec,
                          @Value("${app.deepseek.read-timeout-sec:120}") int readTimeoutSec) {
        this.model = model == null || model.trim().isEmpty() ? "deepseek-chat" : model.trim();
        this.configured = apiKey != null && !apiKey.trim().isEmpty();
        this.maxTokens = Math.max(256, maxTokens);
        this.temperature = temperature;
        this.connectTimeoutSec = connectTimeoutSec;
        this.readTimeoutSec = readTimeoutSec;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSec));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSec));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(factory);
        if (configured) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey.trim());
        }
        this.restClient = builder.build();
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getModel() {
        return model;
    }

    public boolean isReasonerModel() {
        return model.toLowerCase().contains("reasoner");
    }

    public String chat(List<ChatMessage> messages) {
        if (!configured) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "未配置 DeepSeek API Key，请在 application-local.properties 中设置 app.deepseek.api-key");
        }
        ChatCompletionRequest body = ChatCompletionRequest.of(model, messages, maxTokens, temperature, isReasonerModel());
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回为空");
            }
            ChatChoice choice = response.choices.get(0);
            if (choice.message == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 未返回有效内容");
            }
            String content = choice.message.content;
            if (content == null || content.trim().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 未返回有效内容");
            }
            return content.trim();
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "DeepSeek 调用失败：" + ex.getStatusCode().value() + " " + truncate(ex.getResponseBodyAsString(), 200));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DeepSeek 调用异常：" + ex.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "https://api.deepseek.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record ChatMessage(String role, String content) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") Integer maxTokens,
            Double temperature
    ) {
        static ChatCompletionRequest of(String model, List<ChatMessage> messages,
                                        int maxTokens, double temperature, boolean reasoner) {
            if (reasoner) {
                return new ChatCompletionRequest(model, messages, maxTokens, null);
            }
            return new ChatCompletionRequest(model, messages, maxTokens, temperature);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionResponse {
        public List<ChatChoice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatChoice {
        public ResponseMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResponseMessage {
        public String content;
        @JsonProperty("reasoning_content")
        public String reasoningContent;
    }
}
