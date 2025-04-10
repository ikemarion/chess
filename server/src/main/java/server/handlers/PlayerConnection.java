package server.handlers;

/**
 * PlayerConnection: holds info about a user's web socket session
 */
public class PlayerConnection {
    public String authToken;
    public int gameID;
    public String username;
    public String color;
}