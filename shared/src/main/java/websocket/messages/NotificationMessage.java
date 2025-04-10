package websocket.messages;

/**
 * A message the server sends to inform players or observers about some event,
 * e.g. "Alice joined as WHITE" or "Bob resigned."
 */
public class NotificationMessage extends ServerMessage {

    private String message;

    public NotificationMessage(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}