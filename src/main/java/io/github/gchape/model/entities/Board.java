package io.github.gchape.model.entities;

import io.github.gchape.exceptions.InvalidCastlingException;
import io.github.gchape.exceptions.InvalidPromotionException;
import io.github.gchape.exceptions.NoPieceFoundException;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * The Board class represents the chessboard and manages the state of the pieces during a game.
 * It validates moves, handles special moves like castling and promotion, and tracks the movement of pieces.
 */
public class Board {

    private final Map<Piece, Set<String>> blackPieces = new EnumMap<>(Piece.class);
    private final Map<Piece, Set<String>> whitePieces = new EnumMap<>(Piece.class);

    private boolean whiteKingMoved;
    private boolean whiteRookMoved;
    private boolean blackRookMoved;
    private boolean blackKingMoved;

    /**
     * Initializes the board with the standard starting positions for both white and black pieces.
     */
    public Board() {
        whitePieces.put(Piece.KING, new HashSet<>(Set.of("e1")));
        whitePieces.put(Piece.QUEEN, new HashSet<>(Set.of("d1")));
        whitePieces.put(Piece.ROOK, new HashSet<>(Set.of("a1", "h1")));
        whitePieces.put(Piece.BISHOP, new HashSet<>(Set.of("c1", "f1")));
        whitePieces.put(Piece.KNIGHT, new HashSet<>(Set.of("b1", "g1")));
        whitePieces.put(Piece.PAWN, new HashSet<>(Set.of("a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2")));

        blackPieces.put(Piece.KING, new HashSet<>(Set.of("e8")));
        blackPieces.put(Piece.QUEEN, new HashSet<>(Set.of("d8")));
        blackPieces.put(Piece.ROOK, new HashSet<>(Set.of("a8", "h8")));
        blackPieces.put(Piece.BISHOP, new HashSet<>(Set.of("c8", "f8")));
        blackPieces.put(Piece.KNIGHT, new HashSet<>(Set.of("b8", "g8")));
        blackPieces.put(Piece.PAWN, new HashSet<>(Set.of("a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7")));
    }

    /**
     * Returns the map of black pieces and their positions on the board.
     *
     * @return A map of black pieces and their corresponding positions.
     */
    public Map<Piece, Set<String>> getBlackPieces() {
        return blackPieces;
    }

    /**
     * Returns the map of white pieces and their positions on the board.
     *
     * @return A map of white pieces and their corresponding positions.
     */
    public Map<Piece, Set<String>> getWhitePieces() {
        return whitePieces;
    }

    /**
     * Validates whether a move is legal for a specific piece, considering its type, position, and whether it's a capture.
     *
     * @param startSquare The starting square of the piece.
     * @param targetSquare The target square to move the piece to.
     * @param piece The type of piece to move.
     * @param isWhite A boolean indicating whether the piece is white.
     * @param isCapture A boolean indicating whether the move is a capture.
     * @return true if the move is valid, false otherwise.
     */
    public boolean isValidMove(final Square startSquare, final Square targetSquare,
                               final Piece piece, final boolean isWhite, final boolean isCapture) {
        return switch (piece) {
            case KING -> startSquare.isKingMoveTo(targetSquare);
            case QUEEN -> startSquare.isQueenMoveTo(targetSquare);
            case BISHOP -> startSquare.isBishopMoveTo(targetSquare);
            case ROOK -> isVacant(startSquare, targetSquare, piece) && startSquare.isRookMoveTo(targetSquare);
            case KNIGHT -> startSquare.isKnightMoveTo(targetSquare);
            case PAWN -> {
                if (isCapture) {
                    yield startSquare.isPawnCaptureTo(targetSquare, isWhite);
                } else {
                    yield startSquare.isPawnMoveTo(targetSquare, isWhite);
                }
            }
        };
    }

    /**
     * Checks if the path for a rook's move is clear (i.e., no pieces are blocking its path).
     *
     * @param startSquare The starting square of the rook.
     * @param targetSquare The target square to move the rook to.
     * @param piece The type of piece (in this case, a rook).
     * @return true if the path is clear for the rook's move, false otherwise.
     */
    private boolean isVacant(final Square startSquare, final Square targetSquare, final Piece piece) {
        Set<String> allOccupiedSquares = new HashSet<>();
        whitePieces.values().forEach(allOccupiedSquares::addAll);
        blackPieces.values().forEach(allOccupiedSquares::addAll);

        if (piece == Piece.ROOK) {
            if (startSquare.x() == targetSquare.x()) {
                int yStart = Math.min(startSquare.y(), targetSquare.y()) + 1;
                int yEnd = Math.max(startSquare.y(), targetSquare.y());

                return IntStream.range(yStart, yEnd)
                        .mapToObj(y -> (char) (startSquare.x() + 'a') + Integer.toString(y))
                        .noneMatch(allOccupiedSquares::contains);

            } else if (startSquare.y() == targetSquare.y()) {
                int xStart = Math.min(startSquare.x(), targetSquare.x()) + 1;
                int xEnd = Math.max(startSquare.x(), targetSquare.x());

                return IntStream.range(xStart, xEnd)
                        .mapToObj(x -> (char) (x + 'a') + Integer.toString(startSquare.y()))
                        .noneMatch(allOccupiedSquares::contains);
            }
        }

        return false;
    }

    /**
     * Finds the piece to move, considering disambiguation and checking for valid moves.
     *
     * @param piece The type of piece to move.
     * @param targetSquare The target square to move the piece to.
     * @param disambiguation The disambiguation string to distinguish between pieces.
     * @param isWhite A boolean indicating whether the piece is white.
     * @param isCapture A boolean indicating whether the move is a capture.
     * @return The starting square of the piece to move.
     * @throws NoPieceFoundException If no valid piece is found.
     */
    public Square findPiece(final Piece piece, final Square targetSquare, final String disambiguation,
                            final boolean isWhite, final boolean isCapture) {
        return (isWhite ? whitePieces : blackPieces).get(piece)
                .stream()
                .map(Square::new)
                .filter(startSquare -> isValidMove(startSquare, targetSquare, piece, isWhite, isCapture))
                .filter(startSquare -> disambiguation == null
                        || startSquare.toChessNotation().contains(disambiguation))
                .findFirst()
                .orElseThrow(() -> new NoPieceFoundException(piece, targetSquare.toChessNotation()));
    }

    /**
     * Moves a piece on the board, updating its position in the pieces' set and marking the piece's movement.
     *
     * @param currentPieces The map of current pieces and their positions.
     * @param startSquare The starting square of the piece.
     * @param targetSquare The target square to move the piece to.
     * @param isWhite A boolean indicating whether the piece is white.
     * @param piece The type of piece to move.
     */
    public void move(final Map<Piece, Set<String>> currentPieces, final Square startSquare,
                     final Square targetSquare, final boolean isWhite, final Piece piece) {
        currentPieces.get(piece).remove(startSquare.toChessNotation());
        currentPieces.get(piece).add(targetSquare.toChessNotation());

        if (isWhite) {
            switch (piece) {
                case ROOK -> whiteRookMoved = true;
                case KING -> whiteKingMoved = true;
            }
        } else {
            switch (piece) {
                case ROOK -> blackRookMoved = true;
                case KING -> blackKingMoved = true;
            }
        }
    }

    /**
     * Attempts to perform castling for the given player and side (king-side or queen-side).
     *
     * @param isWhite A boolean indicating whether the player is white.
     * @param kingSide A boolean indicating whether the castling is king-side (true) or queen-side (false).
     * @throws InvalidCastlingException If castling is not allowed based on the current game state.
     */
    public void tryCastle(final boolean isWhite, final boolean kingSide) {
        var side = isWhite ? "White" : "Black";
        var direction = kingSide ? "KingSide" : "QueenSide";
        var squares = getCastleType(isWhite, side, direction);

        var currentPieces = isWhite ? whitePieces : blackPieces;
        currentPieces.get(Piece.KING).remove(squares[0]);
        currentPieces.get(Piece.KING).add(squares[1]);
        currentPieces.get(Piece.ROOK).remove(squares[2]);
        currentPieces.get(Piece.ROOK).add(squares[3]);

        if (isWhite) {
            whiteRookMoved = true;
            whiteKingMoved = true;
        } else {
            blackRookMoved = true;
            blackKingMoved = true;
        }
    }

    /**
     * Retrieves the appropriate squares for castling based on the player's color and castling side (king-side or queen-side).
     *
     * @param isWhite A boolean indicating whether the player is white.
     * @param side The side (either "White" or "Black").
     * @param direction The direction (either "KingSide" or "QueenSide").
     * @return An array of strings representing the squares for castling.
     * @throws InvalidCastlingException If castling is not allowed for the given side and direction.
     */
    private String[] getCastleType(final boolean isWhite, final String side, final String direction) {
        if ((isWhite && (whiteKingMoved || whiteRookMoved))
                || (!isWhite && (blackKingMoved || blackRookMoved))) {
            throw new InvalidCastlingException(side, direction);
        }

        var castlingSquares = Map.of("WhiteKingSide", new String[]{"e1", "g1", "h1", "f1"},
                "WhiteQueenSide", new String[]{"e1", "c1", "a1", "d1"},
                "BlackKingSide", new String[]{"e8", "g8", "h8", "f8"},
                "BlackQueenSide", new String[]{"e8", "c8", "a8", "d8"});
        return castlingSquares.get(side + direction);
    }

    /**
     * Attempts to perform pawn promotion when a pawn reaches the last rank.
     *
     * @param isWhite A boolean indicating whether the player is white.
     * @param move The move string containing the pawn promotion.
     * @throws InvalidPromotionException If the promotion is not valid based on the current position.
     * @throws NoPieceFoundException If the pawn to promote cannot be found.
     */
    public void tryPromotion(final boolean isWhite, final String move) {
        var square = move.substring(0, 2);
        var piece = Piece.of(move.charAt(3));
        var currentPieces = isWhite ? whitePieces : blackPieces;
        var pawnPosition = isWhite ? square.charAt(0) + "7" : square.charAt(0) + "2";

        if (isWhite && square.charAt(1) != '8' || !isWhite && square.charAt(1) != '1') {
            throw new InvalidPromotionException(square);
        } else if (!currentPieces.get(Piece.PAWN).contains(pawnPosition)) {
            throw new NoPieceFoundException(Piece.PAWN, pawnPosition);
        }

        currentPieces.get(piece).add(square);
        currentPieces.get(Piece.PAWN).remove(pawnPosition);
    }
}
