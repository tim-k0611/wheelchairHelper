package de.bklk.wheelchairhelper.web;

import de.bklk.wheelchairhelper.model.EmergencyContact;
import de.bklk.wheelchairhelper.service.EmailService;
import de.bklk.wheelchairhelper.service.SupabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    @Autowired
    private SupabaseService supabaseService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<?> saveContact(@RequestBody EmergencyContact contact, Authentication authentication){
        try{
            String userId = (String) authentication.getPrincipal();
            String userToken = (String) authentication.getCredentials();

            EmergencyContact saved = supabaseService.saveEmergencyContact(userId, userToken, contact);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Speichern: " + e.getMessage()));
        }
    }

    /// Notfallkontakt abrufen
    @GetMapping("/contact")
    public ResponseEntity<?> getContact(Authentication authentication){
        try{
           String userId = (String) authentication.getPrincipal();
           String userToken = (String) authentication.getCredentials();
           EmergencyContact contact = supabaseService.getEmergencyContact(userId, userToken);

           if (contact == null){
               return ResponseEntity.notFound().build();
           }

           return ResponseEntity.ok(contact);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Aufrufen: " + e.getMessage()));
        }
    }

    /// Notfall-Endpoint - Sendet E-Mail an Notfallkontakt
    @PostMapping("/trigger")
    public ResponseEntity<?> triggerEmergency(Authentication authentication) {
        try{
            String userId = (String) authentication.getPrincipal();
            String userToken = (String) authentication.getCredentials();

            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId ist erforderlich"));
            }

            EmergencyContact contact = supabaseService.getEmergencyContact(userId, userToken);

            if (contact == null){
                return ResponseEntity.badRequest().body(Map.of("error", "Kein Notfallkontakt hinterlegt"));
            }

            /// E-Mail senden
            emailService.sendEmergencyNotification(
                    contact.getContactEmail(),
                    contact.getContactName(),
                    "Benutzer " + userId
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Notfall-Benachrichtigung versendet",
                    "recipient", contact.getContactEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Fehler beim Versenden: " + e.getMessage()));
        }
    }

    /// Health-Check
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

}
