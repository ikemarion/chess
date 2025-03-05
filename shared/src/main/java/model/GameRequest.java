package model;

public class GameRequest {
    private String gameName;
    private String userId;  // Add more fields if needed

    // Constructors
    public GameRequest(String gameName, String userId) {
        this.gameName = gameName;
        this.userId = userId;
    }

    // Getters and Setters
    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}