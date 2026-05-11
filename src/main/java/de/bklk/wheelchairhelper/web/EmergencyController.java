package de.bklk.wheelchairhelper.web;

import com.google.gson.Gson;
import de.bklk.wheelchairhelper.model.*;
import de.bklk.wheelchairhelper.service.EmailService;
import de.bklk.wheelchairhelper.service.SupabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    @Autowired
    private SupabaseService supabaseService;

    @Autowired
    private EmailService emailService;

    @Value("${device.trigger.secret}")
    private String triggerSecret;

    private final Gson gson = new Gson();

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

    /// Notfall-Endpoint - Sendet E-Mail an Notfallkontakt
    @PostMapping("/device/trigger")
    public ResponseEntity<?> triggerDeviceEmergency(@RequestBody TriggerRequest request) {

        if (!request.triggerSecret().equals(triggerSecret)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Nicht autorisiert"));

        try {
            Optional<Device> existing = supabaseService.getDeviceById(request.deviceId());

            if (existing.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Gerät nicht bekannt"));

            Device device = existing.get();
            if (!device.status().equals(Status.PAIRED) || device.userId() == null || device.userId().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Device ist nicht paired"));

            EmergencyContact contact = supabaseService.getEmergencyContact(device.userId(), triggerSecret);
            UserInformation userInformation = supabaseService.getUserInformation(device.userId(), triggerSecret);

            if (contact == null || userInformation == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Kein Notfallkontakt/Userinformation hinterlegt"));
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
            return ResponseEntity.status(500).body(Map.of("error", "Fehler beim Trigger: " + e.getMessage()));
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
                Map<String, Object> data = new HashMap<>();
                data.put("device_id", deviceRequest.deviceId());
                data.put("last_announce", lastAnnounce);
                saved = supabaseService.updateDevice(data);
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
    public ResponseEntity<?> pairWithDevice(@RequestBody DeviceIdRequest request, Authentication authentication) {
        String userToken = (String) authentication.getCredentials();
        String userId = (String) authentication.getPrincipal();

        try {
            Optional<PairingSession> existing = supabaseService.getPairingSessionForUser(userId, userToken);
            if (existing.isEmpty() || OffsetDateTime.parse(existing.get().expiresAt()).isBefore(OffsetDateTime.now(ZoneOffset.UTC))) return ResponseEntity.status(404).body(Map.of("error", "Fehler beim Senden des Codes: Pairing Session ist abgelaufen"));

            PairingSession session = existing.get();

            Map<String, Object> data = new HashMap<>();
            data.put("device_id", request.deviceId());
            data.put("user_id", userId);
            data.put("expires_at", session.expiresAt());
            PairingSession saved = supabaseService.updatePairingSession(userToken, data);
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

            Optional<PairingSession> existing = supabaseService.getPairingSessionForUser(userId, userToken);

            PairingSession saved;
            if (existing.isPresent()){
                saved = supabaseService.updatePairingSession(userToken, data);
            }else {
                saved = supabaseService.savePairingSession(userToken, data);
            }
            if (saved == null) return ResponseEntity.status(500).body(Map.of("error", "Fehler beim Speichern der Pairing-Session"));
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    @PostMapping("/user/pairing/complete")
    public ResponseEntity<?> completePairing(@RequestBody DeviceIdRequest request, Authentication authentication){
        String userId = (String) authentication.getPrincipal();
        String userToken = (String) authentication.getCredentials();

        try {
            Optional<Device> existing = supabaseService.getDeviceById(request.deviceId());
            if (existing.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Gerät nicht gefunden"));
            Device device = existing.get();
            Map<String, Object> data = new HashMap<>();
            data.put("device_id", device.deviceId());
            data.put("status", Status.PAIRED);
            data.put("user_id", userId);
            Device saved = supabaseService.updateDevice(data);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    @GetMapping("/device/pairing/paired")
    public ResponseEntity<?> isCompletelyPaired(@RequestParam String deviceId){
        try {
            Optional<Device> existing = supabaseService.getDeviceById(deviceId);
            if (existing.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Gerät nicht gefunden"));
            Device device = existing.get();
            if (device.status() != Status.PAIRED) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Gerät ist nicht paired"));
            return ResponseEntity.ok(Map.of(
                    "device_id", device.deviceId(),
                    "status", device.status(),
                    "user_id", device.userId(),
                    "last_announce", device.lastAnnounce(),
                    "trigger_secret", triggerSecret
            ));
        } catch (Exception e){
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Fehler beim Abrufen: " +  e.getMessage()));
        }
    }

    @GetMapping("/user/pairing/device")
    public ResponseEntity<?> getDeviceToUser(Authentication authentication){
        String userId = (String) authentication.getPrincipal();
        String userToken = (String) authentication.getCredentials();

        try{
            Optional<Device> existing = supabaseService.getDeviceForUser(userId, userToken);
            if (existing.isEmpty()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(existing.get());
        }catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    @PostMapping("/user/pairing/disconnect")
    public ResponseEntity<?> disconnectDeviceFromUser(@RequestBody DeviceIdRequest request, Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String userToken = (String) authentication.getCredentials();

        try {
            Optional<Device> existing = supabaseService.getDeviceForUser(userId, userToken);
            if (existing.isEmpty() || !Objects.equals(request.deviceId(), existing.get().deviceId())) return ResponseEntity.notFound().build();
            Device device = existing.get();
            Map<String, Object> data = new HashMap<>();
            data.put("device_id", device.deviceId());
            data.put("status", Status.UNPAIRED);
            data.put("user_id", null);
            Device saved = supabaseService.updateDevice(data);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Fehler beim Speichern: " +  e.getMessage()));
        }
    }

    /// Health-Check
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

}
