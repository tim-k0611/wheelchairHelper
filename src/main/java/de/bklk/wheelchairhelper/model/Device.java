package de.bklk.wheelchairhelper.model;

import com.google.gson.annotations.SerializedName;

public record Device (@SerializedName("device_id") String deviceId, Status status, @SerializedName("user_id") String userId) {
}
