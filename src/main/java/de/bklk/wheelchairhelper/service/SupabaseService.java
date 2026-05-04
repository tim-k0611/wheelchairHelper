package de.bklk.wheelchairhelper.service;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import de.bklk.wheelchairhelper.model.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SupabaseService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    // Notfallkontakt speichern/aktualisieren
    public EmergencyContact saveEmergencyContact(String userId, String userToken, EmergencyContact contact) throws IOException {
        contact.setUserId(userId);

        String json = gson.toJson(contact);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/emergency_contacts?on_conflict=user_id")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Fehler beim Speichern: " + response.code());
            }
            String responseBody = response.body().string();
            EmergencyContact[] contacts = gson.fromJson(responseBody, EmergencyContact[].class);
            return contacts.length > 0 ? contacts[0] : null;
        }
    }

    // Userinformationen speichern/aktualisieren
    public UserInformation saveUserInformation(String userId, String userToken, UserInformation userInformation) throws IOException {
        userInformation.setUserId(userId);

        String json = gson.toJson(userInformation);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/user_information?on_conflict=user_id")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Fehler beim Speichern: " + response.code());
            }
            if (response.body() != null) {
                String responseBody = response.body().string();
                UserInformation[] userInformations = gson.fromJson(responseBody, UserInformation[].class);
                return userInformations.length > 0 ? userInformations[0] : null;
            }
            return null;
        }
    }

    //Userinformationen abrufen
    public UserInformation getUserInformation(String userId, String userToken) throws IOException {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/user_information?user_id=eq." + userId)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Fehler beim Aufrufen: " + response.code());
            }

            if (response.body() != null) {
                String responseBody = response.body().string();
                UserInformation[] userInformations = gson.fromJson(responseBody, UserInformation[].class);
                return userInformations.length > 0 ? userInformations[0] : null;
            }
            return null;
        }
    }

    // Notfallkontakt abrufen
    public EmergencyContact getEmergencyContact(String userId, String userToken) throws IOException {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/emergency_contacts?user_id=eq." + userId)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Fehler beim Abrufen: " + response.code());
            }
            String responseBody = response.body().string();
            EmergencyContact[] contacts = gson.fromJson(responseBody, EmergencyContact[].class);
            return contacts.length > 0 ? contacts[0] : null;
        }
    }

    // Supabase Auth Token validieren
    public boolean validateToken(String token) throws IOException {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/user")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // User ID aus Token extrahieren
    public String getUserIdFromToken(String token) throws IOException {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/user")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                return gson.fromJson(body, UserResponse.class).id;
            }
            return null;
        }
    }

    public Optional<Device> getDeviceById(String deviceId) throws IOException {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/devices?device_id=eq." + deviceId)
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "kein body";
                throw new IOException("Fehler beim Speichern: " + response.code() + " – " + errorBody);
            }
            if (response.body() != null) {
                String body = response.body().string();
                Device[] devices = gson.fromJson(body, Device[].class);
                return devices.length > 0 ? Optional.of(devices[0]) : Optional.empty();
            }
            return Optional.empty();
        }
    }

    public Device saveDevice(Device device) throws IOException{
        DeviceInsert insert = new DeviceInsert(device.deviceId(), device.status(), device.lastAnnounce());
        RequestBody body = RequestBody.create(
                gson.toJson(insert),
                MediaType.parse("application/json")
        );
        System.out.println(gson.toJson(insert));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/devices?on_conflict=device_id")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates, return=representation")
                .build();

        try (Response response = client.newCall(request).execute()){
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "kein body";
                throw new IOException("Fehler beim Speichern: " + response.code() + " – " + errorBody);
            }
            if (response.body() != null) {
                String responseBody = response.body().string();
                Device[] devices = gson.fromJson(responseBody, Device[].class);
                return devices.length > 0 ? devices[0] : null;
            }
            return null;
        }
    }

    public List<Device> getRecentlyAnnouncedDevices() throws IOException {
        String twoMinutesAgo = OffsetDateTime.now(ZoneOffset.UTC)
                .minusMinutes(2)
                .format(DateTimeFormatter.ISO_INSTANT);

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/devices?last_announce=gte." + twoMinutesAgo + "&status=eq.UNPAIRED")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String body = response.body().string();
            Device[] devices = gson.fromJson(body, Device[].class);
            return Arrays.asList(devices);
        }
    }

    public Device updateDevice(String deviceId, String lastAnnounce) throws IOException {
        String json = gson.toJson(new DeviceUpdateLastAnnounce(lastAnnounce));
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/devices?device_id=eq." + deviceId)
                .patch(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String responseBody = response.body().string();
            Device[] devices = gson.fromJson(responseBody, Device[].class);
            return devices.length > 0 ? devices[0] : null;
        }
    }

    public Optional<PairingSession> getPairingSessionForDevice (String deviceId) throws IOException {

        String now = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/pairing_sessions?device_id=eq." + deviceId + "&expires_at=gte." + now + "&order=expires_at.desc&limit=1")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "kein body";
                throw new IOException("Fehler beim Laden: " + response.code() + " – " + errorBody);
            }
            if (response.body() != null) {
                String responseBody = response.body().string();
                PairingSession[] sessions = gson.fromJson(responseBody, PairingSession[].class);
                return sessions.length > 0 ? Optional.of(sessions[0]) : Optional.empty();
            }
            return Optional.empty();
        }
    }

    public Optional<PairingSession> getPairingSessionForUser (String userId, String userToken) throws IOException {

        String now = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/pairing_sessions?user_id=eq." + userId + "&order=expires_at.desc&limit=1")
                .get()
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "kein body";
                throw new IOException("Fehler beim Laden: " + response.code() + " – " + errorBody);
            }
            if (response.body() != null) {
                String responseBody = response.body().string();
                PairingSession[] sessions = gson.fromJson(responseBody, PairingSession[].class);
                return sessions.length > 0 ? Optional.of(sessions[0]) : Optional.empty();
            }
            return Optional.empty();
        }
    }

    public PairingSession savePairingSession(String userToken, Map<String, Object> data) throws IOException {
        String json = gson.toJson(data);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/pairing_sessions")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "kein body";
                throw new IOException("Fehler beim Speichern: " + response.code() + " – " + errorBody);
            }
            if (response.body() != null) {
                String responseBody = response.body().string();
                PairingSession[] sessions = gson.fromJson(responseBody, PairingSession[].class);
                return sessions.length > 0 ? sessions[0] : null;
            }
            return null;
        }
    }

    public PairingSession updatePairingSession(String userToken, Map<String, Object> data) throws IOException {
        String json = gson.toJson(data);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/pairing_sessions?user_id=eq." + data.get("user_id"))
                .patch(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "kein body";
                throw new IOException("Fehler beim Speichern: " + response.code() + " – " + errorBody);
            }
            if (response.body() != null) {
                String responseBody = response.body().string();
                PairingSession[] sessions = gson.fromJson(responseBody, PairingSession[].class);
                return sessions.length > 0 ? sessions[0] : null;
            }
            return null;
        }
    }

    private static class UserResponse {
        String id;
    }

    private record DeviceInsert(
            @SerializedName("device_id") String deviceId,
            Status status,
            @SerializedName("last_announce") String lastAnnounce
            ) {}

    private record DeviceUpdateLastAnnounce(
            @SerializedName("last_announce") String lastAnnounce
    ) {}

}
