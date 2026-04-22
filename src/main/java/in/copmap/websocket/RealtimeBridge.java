package in.copmap.websocket;

import in.copmap.config.RedisConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges Redis pub/sub → STOMP topics so that:
 *   - multiple app replicas can publish location/alerts to Redis
 *   - each replica fans out to its connected WebSocket clients
 *
 * Topics exposed to the frontend:
 *   /topic/locations → live officer pings
 *   /topic/alerts    → alerts (PANIC, geofence breach, etc.)
 */
@Component
@Slf4j
public class RealtimeBridge implements MessageListener {

    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate stomp;

    public RealtimeBridge(RedisMessageListenerContainer container, SimpMessagingTemplate stomp) {
        this.container = container; this.stomp = stomp;
    }

    @PostConstruct
    public void subscribe() {
        container.addMessageListener(this, new PatternTopic(RedisConfig.CHANNEL_LOCATION));
        container.addMessageListener(this, new PatternTopic(RedisConfig.CHANNEL_ALERTS));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        String destination = switch (channel) {
            case RedisConfig.CHANNEL_LOCATION -> "/topic/locations";
            case RedisConfig.CHANNEL_ALERTS   -> "/topic/alerts";
            default -> null;
        };
        if (destination != null) stomp.convertAndSend(destination, body);
    }
}
