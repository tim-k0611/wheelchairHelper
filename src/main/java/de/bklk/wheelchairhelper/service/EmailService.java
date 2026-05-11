package de.bklk.wheelchairhelper.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bklk.wheelchairhelper.model.EmergencyContact;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    public JavaMailSender mailSender;

    @Value("${resend.api.key}")
    private String resendApiKey;

    private final Gson gson = new GsonBuilder()
            .create();

    private final OkHttpClient client = new OkHttpClient();

    public void sendEmergencyNotification(EmergencyContact contact, String userName) throws IOException {
        Map<String, Object> payload = getPayload(contact, userName);

        String json = gson.toJson(payload);

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.resend.com/emails")
                .addHeader("Authorization", "Bearer " + resendApiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try(Response response = client.newCall(request).execute();) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null
                        ? response.body().string()
                        : "Keine Fehlermeldung";
                throw new IOException(
                        "Fehler beim Versenden der E-Mail: "
                                + response.code()
                                + " - "
                                + errorBody
                );
            }
        }
    }

    private static @NonNull Map<String, Object> getPayload(EmergencyContact contact, String userName) {
        String message = """
    <p>Hallo %s %s,</p>

    <p>
        dies ist eine automatische
        <strong>Notfall-Benachrichtigung</strong>.
    </p>

    <p>
        Ein Notfall-Signal wurde für
        <strong>"%s"</strong>
        ausgelöst.
    </p>

    <p>
        Bitte nimm so schnell wie möglich Kontakt auf.
    </p>

    <hr>

    <p style="font-size:12px;color:gray;">
        Diese Nachricht wurde automatisch generiert.
    </p>
    """.formatted(
                contact.getContactFirstName(),
                contact.getContactName(),
                userName
        );

        return Map.of(
                "from", "onboarding@resend.dev",
                "to", contact.getContactEmail(),
                "subject", "🚨 Notfall-Benachrichtigung",
                "html", message
        );
    }

    public void sendTestEmail(String recipientEmail, String recipientName) {
        try {
            System.out.println("=== Sending Email ===");
            System.out.println("To: " + recipientEmail);
            System.out.println("Name: " + recipientName);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("✅ Test-Benachrichtigung");
            message.setText(
                    "Hallo " + recipientName + ",\n\n" +
                            "dies ist eine Test-Benachrichtigung.\n\n" +
                            "Deine E-Mail-Adresse wurde erfolgreich als Notfallkontakt hinterlegt.\n\n" +
                            "---\n" +
                            "Notfall-Benachrichtigungssystem"
            );

            mailSender.send(message);
            System.out.println("Email sent successfully!");

        } catch (Exception e) {
            System.err.println("Email sending failed: " + e.getMessage());
            e.printStackTrace();
            throw e;  // Weitergeben, damit Controller es sieht
        }
    }
}
