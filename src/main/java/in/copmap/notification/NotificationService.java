package in.copmap.notification;

import in.copmap.domain.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Pluggable notification dispatch. Configured via `copmap.notification.channel`:
 *   - "log"            → structured log line (default, CI-friendly)
 *   - "email"          → SMTP via Spring Mail
 *   - "mock-whatsapp"  → log line formatted as a WhatsApp payload
 *
 * Kept deliberately simple — real integrations (FCM, Twilio, Gupshup) would swap in here.
 */
@Service
@Slf4j
public class NotificationService {

    private final String channel;
    private final String fromEmail;
    private final JavaMailSender mailSender;

    public NotificationService(@Value("${copmap.notification.channel}") String channel,
                               @Value("${copmap.notification.from-email:no-reply@copmap.in}") String fromEmail,
                               @org.springframework.beans.factory.annotation.Autowired(required = false)
                               JavaMailSender mailSender) {
        this.channel = channel;
        this.fromEmail = fromEmail;
        this.mailSender = mailSender;
    }

    @Async
    public void dispatch(Alert a) {
        String summary = "[%s/%s] %s @ (%s,%s): %s".formatted(
                a.getType(), a.getSeverity(),
                a.getOfficerId(), a.getLatitude(), a.getLongitude(), a.getMessage());
        switch (channel) {
            case "email" -> sendEmail(summary);
            case "mock-whatsapp" -> log.info("[WHATSAPP-MOCK] → planner-on-call: {}", summary);
            default -> log.info("[ALERT-NOTIFY] {}", summary);
        }
    }

    private void sendEmail(String body) {
        if (mailSender == null) { log.warn("email channel configured but JavaMailSender unavailable"); return; }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(fromEmail);
            m.setTo("planner-on-call@copmap.in");
            m.setSubject("CopMap Alert");
            m.setText(body);
            mailSender.send(m);
        } catch (Exception e) {
            log.warn("Email dispatch failed: {}", e.getMessage());
        }
    }
}
