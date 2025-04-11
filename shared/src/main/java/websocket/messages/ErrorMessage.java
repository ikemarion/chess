package websocket.messages;

/**
 * A server message with serverMessageType = ERROR,
 * carrying a short error message string.
 */
public class ErrorMessage extends ServerMessage {

    private String errorMessage; // must contain the word "Error"

    public ErrorMessage(String errorMessage) {
        super(ServerMessageType.ERROR);
        this.errorMessage = errorMessage;
    }

    public ErrorMessage() {
        super(ServerMessageType.ERROR);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }
}