package model;

public class GameRequest {
    private String gameName;
    private String userId;

    public GameRequest(String gameName, String userId) {
        this.gameName = gameName;
        this.userId = userId;
    }

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