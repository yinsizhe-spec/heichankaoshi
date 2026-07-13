package com.heichan.camera.camera.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * QuickRouter OpenAI 兼容请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuickRouterChatRequest {

    private String model;

    private List<Message> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Boolean stream;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {

        private String role;

        /**
         * system 消息为字符串；
         * user 多模态消息为 ContentPart 数组。
         */
        private Object content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentPart {

        /**
         * text 或 image_url。
         */
        private String type;

        private String text;

        @JsonProperty("image_url")
        private ImageUrl imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageUrl {

        private String url;

        /**
         * low、high、auto。
         */
        private String detail;
    }
}