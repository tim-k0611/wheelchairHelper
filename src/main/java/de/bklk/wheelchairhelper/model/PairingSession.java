package de.bklk.wheelchairhelper.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

public record PairingSession(@SerializedName("user_id") String userId, int code, @SerializedName("expires_at") String expiresAt) {
}
