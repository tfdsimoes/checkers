package com.tfdsimoes.checkers.game;

public enum GameMode {
    PLAYER_VS_PLAYER,
    PLAYER_VS_MACHINE,
    MACHINE_VS_MACHINE;

    @Override
    public String toString() {
        return switch (this) {
            case PLAYER_VS_PLAYER -> "Player vs Player";
            case PLAYER_VS_MACHINE -> "Player vs Machine";
            case MACHINE_VS_MACHINE -> "Machine vs Machine";
        };
    }
}
