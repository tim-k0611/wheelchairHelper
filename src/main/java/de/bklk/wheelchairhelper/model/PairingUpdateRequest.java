package de.bklk.wheelchairhelper.model;

import com.google.gson.annotations.SerializedName;

public record PairingUpdateRequest (@SerializedName("device_id") String deviceId)
{}
