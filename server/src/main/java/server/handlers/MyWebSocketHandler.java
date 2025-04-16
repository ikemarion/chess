package server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket
public class MyWebSocketHandler {

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket /ws connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        System.out.println("WebSocket /ws received: " + message);

        // Send back the exact JSON the client sent:
        try {
            session.getRemote().sendString(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket /ws closed: " + reason);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.out.println("WebSocket /ws error: " + error.getMessage());
    }
}