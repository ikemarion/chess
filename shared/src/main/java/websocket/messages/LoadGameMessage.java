package websocket.messages;

import model.GameData;

/**
 * A server message with serverMessageType = LOAD_GAME,
 * carrying the full GameData.
 */
public class LoadGameMessage extends ServerMessage {

    public LoadGameMessage(GameData updatedGame) {
        // Pass null for recipient; your send logic can fill this later from sessionUserMap.
        super(ServerMessageType.LOAD_GAME, updatedGame, null);
    }

    // For GSON
    public LoadGameMessage() {
        super(ServerMessageType.LOAD_GAME);
    }
}