package de.bklk.wheelchairhelper.service;

import com.google.gson.Gson;
import de.bklk.wheelchairhelper.model.EmergencyContact;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
                .url(supabaseUrl + "/rest/v1/emergency_contacts")
                .post(body)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
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

    private static class UserResponse {
        String id;
    }
}
