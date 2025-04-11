package websocket.messages;

import model.GameData;

/**
 * A server message with serverMessageType = LOAD_GAME,
 * carrying the full GameData.
 */
public class LoadGameMessage extends ServerMessage {

    public LoadGameMessage(GameData updatedGame) {
        super(ServerMessageType.LOAD_GAME, updatedGame);
    }

    // For GSON
    public LoadGameMessage() {
        super(ServerMessageType.LOAD_GAME);
    }
}