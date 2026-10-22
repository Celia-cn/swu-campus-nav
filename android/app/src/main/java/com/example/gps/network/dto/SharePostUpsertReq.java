package com.example.gps.network.dto;

public class SharePostUpsertReq {
    public String category;
    public String title;
    public String content;
    public String imageUrl;
    public boolean titleAsImage;

    public SharePostUpsertReq(String category, String title, String content, String imageUrl, boolean titleAsImage) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.titleAsImage = titleAsImage;
    }
}
