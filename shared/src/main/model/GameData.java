package model;

import chess.ChessGame; // Assuming this is provided in the starter code.

public record GameData(int gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {}