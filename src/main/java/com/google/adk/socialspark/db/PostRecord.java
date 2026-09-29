package com.google.adk.socialspark.db;

import com.google.gson.annotations.SerializedName;

public record PostRecord(
        long id,
        String platform,
        String text,
        @SerializedName("image_path") String imagePath,
        @SerializedName("image_url") String imageUrl,
        @SerializedName("post_url") String postUrl,
        @SerializedName("created_at") String createdAt
) {}
