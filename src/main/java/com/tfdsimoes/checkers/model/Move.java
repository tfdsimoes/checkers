package com.tfdsimoes.checkers.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record Move(List<Position> path, List<Position> captures) {
    public Move {
        path = List.copyOf(Objects.requireNonNull(path, "path"));
        captures = List.copyOf(Objects.requireNonNull(captures, "captures"));
        if (path.size() < 2) {
            throw new IllegalArgumentException("A move must contain a source and destination");
        }
        if (path.stream().anyMatch(position -> !position.isPlayable())
                || captures.stream().anyMatch(position -> !position.isPlayable())) {
            throw new IllegalArgumentException("Moves can only use playable squares");
        }
        if (!captures.isEmpty() && captures.size() != path.size() - 1) {
            throw new IllegalArgumentException("Every jump in a capture must capture one piece");
        }
        if (captures.isEmpty() && path.size() != 2) {
            throw new IllegalArgumentException("A non-capturing move has one destination");
        }
    }

    public Position from() {
        return path.get(0);
    }

    public Position to() {
        return path.get(path.size() - 1);
    }

    public boolean isCapture() {
        return !captures.isEmpty();
    }

    public String notation() {
        String separator = isCapture() ? "x" : "-";
        return path.stream()
                .map(position -> Integer.toString(position.squareNumber()))
                .collect(Collectors.joining(separator));
    }
}
