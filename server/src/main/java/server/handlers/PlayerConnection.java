package server.handlers;

/**
 * PlayerConnection: holds info about a user's web socket session
 */
public class PlayerConnection {
    public String authToken;
    public int gameID;
    public String username;  // once you figure it out from DB
    public String color;     // "WHITE", "BLACK", or "OBSERVER"
}