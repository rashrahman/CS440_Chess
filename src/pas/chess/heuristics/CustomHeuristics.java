package src.pas.chess.heuristics;


// SYSTEM IMPORTS
import edu.bu.chess.search.DFSTreeNode;


import edu.bu.chess.game.piece.Piece;
import edu.bu.chess.game.piece.PieceType;
import edu.bu.chess.game.player.Player;
import edu.bu.chess.utils.Coordinate;
import edu.cwru.sepia.util.Direction;
// JAVA PROJECT IMPORTS
import src.pas.chess.heuristics.DefaultHeuristics;


public class CustomHeuristics
    extends Object
{

	/**
	 * TODO: implement me! The heuristics that I wrote are useful, but not very good for a good chessbot.
	 * Please use this class to add your heuristics here! I recommend taking a look at the ones I provided for you
	 * in DefaultHeuristics.java (which is in the same directory as this file)
	 */
	public static double getMaxPlayerHeuristicValue(DFSTreeNode node)
	{
        double materialValue = getMaterialValue(node);
        double pieceSafety = getPieceSafetyValue(node);
        double boardControl = getBoardControlValue(node);
        double mobility = getMobilityValue(node);
        double pawnStructure = getPawnStructureValue(node);

        // Combining all heuristic components with weights
        return 0.4 * materialValue + 
               0.3 * pieceSafety + 
               0.2 * boardControl + 
               0.1 * mobility + 
               0.1 * pawnStructure;
    }

    // 1. Material value heuristic
    private static double getMaterialValue(DFSTreeNode node) {
        double totalValue = 0;
        for (Piece piece : node.getGame().getBoard().getPieces(DefaultHeuristics.getMaxPlayer(node))) {
            totalValue += Piece.getPointValue(piece.getType());
        }
        return totalValue;
    }

    // 2. Piece safety heuristic (penalizes exposed pieces)
    private static double getPieceSafetyValue(DFSTreeNode node) {
        int threatenedPieces = DefaultHeuristics.DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node);
        return -5 * threatenedPieces; // Penalize based on number of threatened pieces
    }

    // 3. Board control heuristic (reward controlling center squares)
	private static double getBoardControlValue(DFSTreeNode node) {
		int controlValue = 0;
		Player maxPlayer = DefaultHeuristics.getMaxPlayer(node);
	
		// Central squares to reward control
		Coordinate[] centerSquares = {
			new Coordinate(3, 3), new Coordinate(3, 4),
			new Coordinate(4, 3), new Coordinate(4, 4)
		};
	
		for (Coordinate square : centerSquares) {
			if (node.getGame().getBoard().isPositionOccupied(square)) {
				Piece piece = node.getGame().getBoard().getPieceAtPosition(square);
	
				// Check if the piece belongs to the max player
				if (piece.getPlayer().equals(maxPlayer)) {
					controlValue += 10; // Reward controlling the center
				}
			}
		}
		return controlValue;
	}
	

    // 4. Mobility heuristic (encourage more move options)
    private static double getMobilityValue(DFSTreeNode node) {
        int mobilityScore = 0;
        for (Piece piece : node.getGame().getBoard().getPieces(DefaultHeuristics.getMaxPlayer(node))) {
            mobilityScore += piece.getAllCaptureMoves(node.getGame()).size();
        }
        return mobilityScore;
    }

    // 5. Pawn structure heuristic (reward chains and penalize isolated pawns)
	private static double getPawnStructureValue(DFSTreeNode node) {
		int pawnScore = 0;
		Player maxPlayer = DefaultHeuristics.getMaxPlayer(node);

		// Iterate through all pawns of the max player
		for (Piece pawn : node.getGame().getBoard().getPieces(maxPlayer, PieceType.PAWN)) {
			Coordinate pos = node.getGame().getCurrentPosition(pawn);

			// Check for connected pawns (left and right neighbors)
			Coordinate leftNeighbor = pos.getNeighbor(Direction.WEST, 1);
			Coordinate rightNeighbor = pos.getNeighbor(Direction.EAST, 1);

			if (isFriendlyPawn(node, leftNeighbor, maxPlayer) || isFriendlyPawn(node, rightNeighbor, maxPlayer)) {
				pawnScore += 5; // Bonus for connected pawns
			} else {
				pawnScore -= 3; // Penalty for isolated pawns
			}
		}
		return pawnScore;
	}

	// Helper method to determine if a neighbor contains a friendly pawn
	private static boolean isFriendlyPawn(DFSTreeNode node, Coordinate coord, Player maxPlayer) {
		if (node.getGame().getBoard().isInbounds(coord) && 
			node.getGame().getBoard().isPositionOccupied(coord)) {

			Piece neighborPiece = node.getGame().getBoard().getPieceAtPosition(coord);
			return neighborPiece.getPlayer().equals(maxPlayer) && 
				neighborPiece.getType() == PieceType.PAWN;
		}
		return false;
	}
}