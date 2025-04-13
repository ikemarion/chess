import java.net.URI;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

@ClientEndpoint
public class GameWebSocketClient {

    private Session session;
    private MessageHandler messageHandler;

    public GameWebSocketClient(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            // Connect to the server endpoint. This method blocks until connected.
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to WebSocket server: " + e.getMessage(), e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("WebSocket connected!");
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received message from server: " + message);
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("WebSocket closed: " + reason.getReasonPhrase());
        this.session = null;
    }

    /**
     * Send a message through the open WebSocket connection.
     */
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        } else {
            System.err.println("Cannot send message; WebSocket session is closed.");
        }
    }

    /**
     * Set a message handler to process messages received from the server.
     */
    public void addMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    /**
     * Interface to handle messages received from the server.
     */
    public static interface MessageHandler {
        void handleMessage(String message);
    }
}