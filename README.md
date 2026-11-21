# Checkers

An English draughts game written in Java 25 with a Swing interface.

## Run

Install Java 25 or newer and Apache Maven, then build and launch the executable JAR:

```text
mvn clean package
java -jar target/checkers-1.0.0.jar
```

The project intentionally does not use the Maven wrapper.

## Game modes

- **Player vs Player** - two people share the same board.
- **Player vs Machine** - choose Easy, Medium, or Hard and whether the human plays Black or White.
- **Machine vs Machine** - two Hard-level strategies play automatically.

Black always moves first. After a finished game, the players' colours are swapped for the next game. An optional numeric seed in the menu makes strategy selection, tie-breaking, and random play reproducible; leaving it blank generates a seed that is displayed during the game.

The board supports click-to-move input, multi-jump selection, pause/resume, undo (one ply in Player vs Player and the human turn in Player vs Machine), and a move-history panel. Machine searches run off the Swing event thread. Machine vs Machine games are paced so their moves can be followed.

## Rules

- Play takes place on the dark squares of an 8x8 board, with 12 pieces per side.
- Men move and capture diagonally forward; kings move and capture in either direction.
- Captures are mandatory. A multi-jump must be completed, and promotion during a jump ends that sequence.
- A player wins when the opponent has no pieces or no legal move.
- In Player vs Player, the players can agree to a draw. All modes also draw after three repetitions of the same position with the same side to move, or after 80 plies without a capture or man move.
- Squares use standard 1-32 numbering. Move history uses `from-to` for a simple move and `fromxto` for a capture; for example, `9-13` or `23x14x5`.

## Machine strategies

Each machine chooses a strategy uniformly from its difficulty pool when a game is created. All search budgets use deterministic depth, node, or iteration limits rather than wall-clock budgets.

| Difficulty | Strategy pool |
|---|---|
| Easy | Random move; greedy one-ply capture; minimax depth 2 |
| Medium | Alpha-beta depth 4; alpha-beta depth 6 with move ordering; MCTS with 1,000 iterations |
| Hard | Iterative-deepening alpha-beta with a transposition table and killer moves (maximum depth 8); alpha-beta with quiescence search (maximum 10,000 extra nodes per move); MCTS with 5,000 iterations |

The default test suite checks legal moves, rule edge cases, and deterministic selection. The slow strength test checks that each Hard strategy wins at least 16 of 20 seeded games against the Easy pool while losing no more than two; run it with `mvn -B test -Dgroups=slow`. It plays 60 full games and takes about 13 minutes.

## Project layout

- `model` - board, pieces, positions, and moves.
- `rules` - legal move generation and move application.
- `game` - game state, draw/win detection, seeds, and player controllers.
- `ai` - evaluation and machine strategies.
- `ui` - Swing menu and board interface.

Run the unit tests with `mvn test`.
