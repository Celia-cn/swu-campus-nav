package com.example.gps.network.dto;

import java.util.ArrayList;
import java.util.List;

public class AiChatReq {
    public String message;
    public List<ChatMessageItem> history;

    public AiChatReq(String message, List<ChatMessageItem> history) {
        this.message = message;
        this.history = history == null ? new ArrayList<>() : history;
    }

    public static class ChatMessageItem {
        public String role;
        public String content;

        public ChatMessageItem(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
