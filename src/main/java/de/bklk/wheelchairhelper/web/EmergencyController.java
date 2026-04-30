package de.bklk.wheelchairhelper.web;

import de.bklk.wheelchairhelper.model.*;
import de.bklk.wheelchairhelper.service.EmailService;
import de.bklk.wheelchairhelper.service.SupabaseService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
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

    @PostMapping("/userinformation")
    public ResponseEntity<?> saveUserInformation(@RequestBody UserInformation userInformation, Authentication authentication){
        try{
            String userId = (String) authentication.getPrincipal();
            String userToken = (String) authentication.getCredentials();

            UserInformation saved = supabaseService.saveUserInformation(userId, userToken, userInformation);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    @GetMapping("/userinformation")
    public ResponseEntity<?> getUserInformation(Authentication authentication){
        try {
            String userId = (String) authentication.getPrincipal();
            String userToken = (String) authentication.getCredentials();

            UserInformation userInformation = supabaseService.getUserInformation(userId, userToken);

            if (userInformation == null) return ResponseEntity.notFound().build();

            return ResponseEntity.ok(userInformation);
        } catch(Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Aufrufen: " + e.getMessage()));
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
            UserInformation userInformation = supabaseService.getUserInformation(userId, userToken);

            if (contact == null){
                return ResponseEntity.badRequest().body(Map.of("error", "Kein Notfallkontakt hinterlegt"));
            }

            /// E-Mail senden
            emailService.sendEmergencyNotification(
                    contact,
                    userInformation.getFirstName() + " " + userInformation.getLastName()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Notfall-Benachrichtigung versendet",
                    "recipient", contact.getContactEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Fehler beim Versenden: " + e.getMessage()));
        }
    }

    /// Gerät meldet sich existent
    @PostMapping("/device/announce")
    public ResponseEntity<?> announceDevice(@RequestBody DeviceAnnouncementRequest deviceRequest) {
        try {
            Optional<Device> existing = supabaseService.getDeviceById(deviceRequest.deviceId());
            String lastAnnounce = OffsetDateTime.now(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
            Device saved;
            if (existing.isPresent()) {
                saved = supabaseService.updateDevice(deviceRequest.deviceId(), lastAnnounce);
            } else {
                Device device = new Device(deviceRequest.deviceId(), Status.UNPAIRED, null, lastAnnounce);
                saved = supabaseService.saveDevice(device);
            }
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    /// Aktive Geräte holen
    @GetMapping("/device/pairing")
    public ResponseEntity<?> getPairingDevices(Authentication authentication){
        try {
            List<Device> devices = supabaseService.getRecentlyAnnouncedDevices();
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /// Falls ein User ein Gerät anklickt zum Koppeln
    @PostMapping("/user/pairing/device")
    public ResponseEntity<?> pairWithDevice(@RequestBody PairingUpdateRequest request, Authentication authentication) {
        String userToken = (String) authentication.getCredentials();
        String userId = (String) authentication.getPrincipal();

        try {
            Optional<PairingSession> existing = supabaseService.getPairingSessionForUser(userId);
            if (existing.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Fehler beim Senden des Codes: Pairing Session ist abgelaufen"));

            PairingSession session = existing.get();

            Map<String, Object> data = new HashMap<>();
            data.put("device_id", request.deviceId());
            data.put("user_id", userId);
            data.put("expires_at", session.expiresAt());
            PairingSession saved = supabaseService.savePairingSession(userToken, data);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    /// Gibt es einen Code für das Gerät?
    @GetMapping("/device/pairing/code")
    public ResponseEntity<?> getDevicePairingCode(@RequestParam String deviceId) {
        try {
            Optional<Device> existing = supabaseService.getDeviceById(deviceId);
            if (existing.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Gerät nicht bekannt"));

            Optional<PairingSession> session = supabaseService.getPairingSessionForDevice(deviceId);

            if (session.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Keine offene Pairing-Session"));

            return ResponseEntity.ok(session.get());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Fehler beim Abrufen des Pairing-Codes: " +  e.getMessage()));
        }
    }

    @PostMapping("/user/pairing/start")
    public ResponseEntity<?> startPairing(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String userToken = (String) authentication.getCredentials();

        try {
            SecureRandom random = new SecureRandom();
            int code = 1000 + random.nextInt(9000);

            String expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(2).format(DateTimeFormatter.ISO_INSTANT);

            Map<String, Object> data = new HashMap<>();
            data.put("user_id", userId);
            data.put("device_id", null);
            data.put("code", code);
            data.put("expires_at", expiresAt);

            PairingSession saved = supabaseService.savePairingSession(userToken, data);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    /// Health-Check
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

}
