package websocket.messages;

import model.GameData;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class ServerMessage {

    private ServerMessageType serverMessageType;

    private String message;

    private String errorMessage;

    private GameData game;

    private String recipient;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

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

    public ServerMessage(ServerMessageType type, GameData game, String recipient) {
        this.serverMessageType = type;
        this.game = game;
        this.recipient = recipient;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o){return true;}
        if (!(o instanceof ServerMessage that)){return false;}
        return serverMessageType == that.serverMessageType &&
                Objects.equals(recipient, that.recipient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverMessageType, recipient);
    }
}