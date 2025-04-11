package websocket.messages;

/**
 * A server message with serverMessageType = NOTIFICATION,
 * carrying a simple message string for all to see.
 */
public class NotificationMessage extends ServerMessage {

    private String message; // e.g. "Alice joined as White"

    public NotificationMessage(String message) {
        super(ServerMessageType.NOTIFICATION);
        this.message = message;
    }

    public NotificationMessage() {
        super(ServerMessageType.NOTIFICATION);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String msg) {
        this.message = msg;
    }
}