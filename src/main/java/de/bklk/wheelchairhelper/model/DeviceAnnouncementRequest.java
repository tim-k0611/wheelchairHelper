package de.bklk.wheelchairhelper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceAnnouncementRequest(@JsonProperty("device_id") String deviceId) {
}
