package de.bklk.wheelchairhelper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    public JavaMailSender mailSender;

    public void sendEmergencyNotification(String recipientEmail, String recipientName, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("🚨 Notfall-Benachrichtigung");
        message.setText(
                "Hallo " + recipientName + ",\n\n" +
                        "dies ist eine automatische Notfall-Benachrichtigung.\n\n" +
                        "Ein Notfall-Signal wurde für \"" + userName + "\" ausgelöst.\n\n" +
                        "Bitte nimm so schnell wie möglich Kontakt auf.\n\n" +
                        "---\n" +
                        "Diese Nachricht wurde automatisch generiert."
        );

        mailSender.send(message);
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
