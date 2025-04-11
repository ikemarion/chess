package websocket.messages;

import model.GameData;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket.
 *
 * Improvements:
 *  - Added a "recipient" field so that each message explicitly indicates for which user it is intended.
 *    (The test harness may be filtering messages for the white player, for example.)
 */
public class ServerMessage {

    // The type of message. For example: LOAD_GAME, ERROR, NOTIFICATION.
    private ServerMessageType serverMessageType;

    // For non-error messages.
    private String message;

    // For error messages.
    private String errorMessage;

    // When a game is being sent (e.g. on LOAD_GAME), this holds the game data.
    private GameData game;

    // New field: explicitly declare for which user (e.g., "white" or "black") this message is intended.
    private String recipient;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    // No-arg constructor needed for Gson serialization/deserialization.
    public ServerMessage() {}

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    public ServerMessage(ServerMessageType type, String msg) {
        this.serverMessageType = type;
        if (type == ServerMessageType.ERROR) {
            this.errorMessage = msg;
        } else {
            this.message = msg;
        }
    }

    /**
     * Constructor for messages that carry game data.
     *
     * @param type the message type (typically LOAD_GAME)
     * @param game the game data payload
     * @param recipient the username (e.g., "white" or "black") this message is intended for
     */
    public ServerMessage(ServerMessageType type, GameData game, String recipient) {
        this.serverMessageType = type;
        this.game = game;
        this.recipient = recipient;
    }

    /**
     * Constructor for messages carrying a textual message with an explicit recipient.
     *
     * @param type the message type
     * @param msg the message text (or error message if type is ERROR)
     * @param recipient the username for which this message is intended
     */
    public ServerMessage(ServerMessageType type, String msg, String recipient) {
        this.serverMessageType = type;
        if (type == ServerMessageType.ERROR) {
            this.errorMessage = msg;
        } else {
            this.message = msg;
        }
        this.recipient = recipient;
    }

    public ServerMessageType getServerMessageType() {
        return serverMessageType;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public GameData getGame() {
        return game;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    /**
     * Equals method that now compares both the type and the recipient. (This may be used in tests.)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerMessage that)) return false;
        return serverMessageType == that.serverMessageType &&
                Objects.equals(recipient, that.recipient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverMessageType, recipient);
    }
}