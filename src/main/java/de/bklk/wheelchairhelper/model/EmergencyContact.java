package de.bklk.wheelchairhelper.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("contact_name")
    private String contactName;

    @SerializedName("contact_first_name")
    private String contactFirstName;

    @SerializedName("contact_email")
    private String contactEmail;

    @SerializedName("contact_phone")
    private String contactPhone;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;
}

