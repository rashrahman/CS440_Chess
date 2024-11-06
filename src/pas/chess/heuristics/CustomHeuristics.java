package src.pas.chess.heuristics;


// SYSTEM IMPORTS
import edu.cwru.sepia.util.Direction;
import edu.bu.chess.game.move.CastleMove;
import edu.bu.chess.game.move.PromotePawnMove;
import edu.bu.chess.game.piece.Piece;
import edu.bu.chess.game.piece.PieceType;
import edu.bu.chess.game.player.Player;
import edu.bu.chess.search.DFSTreeNode;
import edu.bu.chess.utils.Coordinate;


// JAVA PROJECT IMPORTS


public class CustomHeuristics
    extends Object
{

	/**
	 * Get the max player from a node
	 * @param node
	 * @return
	 */
	public static Player getMaxPlayer(DFSTreeNode node)
	{
		return node.getMaxPlayer();
	}

	/**
	 * Get the min player from a node
	 * @param node
	 * @return
	 */
	public static Player getMinPlayer(DFSTreeNode node)
	{
		return CustomHeuristics.getMaxPlayer(node).equals(node.getGame().getCurrentPlayer()) ? node.getGame().getOtherPlayer() : node.getGame().getCurrentPlayer();
	}


	public static class OffensiveHeuristics extends Object
	{

		public static int getNumberOfPiecesMaxPlayerIsThreatening(DFSTreeNode node)
		{

			int numPiecesMaxPlayerIsThreatening = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(CustomHeuristics.getMaxPlayer(node)))
			{
				numPiecesMaxPlayerIsThreatening += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesMaxPlayerIsThreatening;
		}

		public static int getCapturePotential(DFSTreeNode node) {
			// Calculates the potential captures by the max player's pieces.
			int captureCount = 0;
			for (Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node))) {
				captureCount += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return captureCount;
		}
		public static int evaluateAggressiveCastling(DFSTreeNode node) {
			 // Evaluates castling towards aggressive (central) positions.
			if (node.getMove() instanceof CastleMove) {
				CastleMove move = (CastleMove) node.getMove();
				Coordinate kingPos = move.getFinalKingPosition();
				// Reward castling to central squares and rooks in open positions
				return (kingPos.getXPosition() == 3 || kingPos.getXPosition() == 6) ? 5 : 0;
			}
			return 0;
}

	}

	public static class DefensiveHeuristics extends Object
	{

		public static int getNumberOfMaxPlayersAlivePieces(DFSTreeNode node)
		{
			 // Counts the number of pieces still alive for the max player.
			int numMaxPlayersPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numMaxPlayersPiecesAlive += node.getGame().getNumberOfAlivePieces(CustomHeuristics.getMaxPlayer(node), pieceType);
			}
			return numMaxPlayersPiecesAlive;
		}

		public static int getNumberOfMinPlayersAlivePieces(DFSTreeNode node)
		{
			 // Counts the number of pieces still alive for the min player
			int numMaxPlayersPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numMaxPlayersPiecesAlive += node.getGame().getNumberOfAlivePieces(CustomHeuristics.getMinPlayer(node), pieceType);
			}
			return numMaxPlayersPiecesAlive;
		}

		public static int getClampedPieceValueTotalSurroundingMaxPlayersKing(DFSTreeNode node)
		{
			// what is the state of the pieces next to the king? add up the values of the neighboring pieces
			// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
			int maxPlayerKingSurroundingPiecesValueTotal = 0;

			Piece kingPiece = node.getGame().getBoard().getPieces(CustomHeuristics.getMaxPlayer(node), PieceType.KING).iterator().next();
			Coordinate kingPosition = node.getGame().getCurrentPosition(kingPiece);
			for(Direction direction : Direction.values())
			{
				Coordinate neightborPosition = kingPosition.getNeighbor(direction);
				if(node.getGame().getBoard().isInbounds(neightborPosition) && node.getGame().getBoard().isPositionOccupied(neightborPosition))
				{
					Piece piece = node.getGame().getBoard().getPieceAtPosition(neightborPosition);
					int pieceValue = Piece.getPointValue(piece.getType());
					if(piece != null && kingPiece.isEnemyPiece(piece))
					{
						maxPlayerKingSurroundingPiecesValueTotal -= pieceValue;
					} else if(piece != null && !kingPiece.isEnemyPiece(piece))
					{
						maxPlayerKingSurroundingPiecesValueTotal += pieceValue;
					}
				}
			}
			// kingSurroundingPiecesValueTotal cannot be < 0 b/c the utility of losing a game is 0, so all of our utility values should be at least 0
			maxPlayerKingSurroundingPiecesValueTotal = Math.max(maxPlayerKingSurroundingPiecesValueTotal, 0);
			return maxPlayerKingSurroundingPiecesValueTotal;
		}

		public static int getNumberOfPiecesThreateningMaxPlayer(DFSTreeNode node)
		{
			// how many pieces are threatening us?
			int numPiecesThreateningMaxPlayer = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(CustomHeuristics.getMinPlayer(node)))
			{
				numPiecesThreateningMaxPlayer += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesThreateningMaxPlayer;
		}
		public static int evaluateKingSafety(DFSTreeNode node) {
			 // Evaluates the safety of the max player's king based on nearby enemy threats.
			Piece king = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.KING).iterator().next();
			Coordinate kingPosition = king.getCurrentPosition(node.getGame().getBoard());
			int safetyScore = 0;
			// Check how many opponent pieces can reach adjacent squares
			for (Piece enemy : node.getGame().getBoard().getPieces(getMinPlayer(node))) {
				if (!enemy.getAllCaptureMoves(node.getGame()).isEmpty()) {
					safetyScore -= enemy.getAllCaptureMoves(node.getGame()).size();
				}
			}
			return safetyScore;
		}
		public static int getKingShieldValue(DFSTreeNode node) {
			 // Computes the value of the pieces acting as a shield for the max player's king.
			Piece king = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.KING).iterator().next();
			Coordinate kingPosition = king.getCurrentPosition(node.getGame().getBoard());
			int shieldValue = 0;
			for (Direction dir : Direction.values()) {
				Coordinate neighbor = kingPosition.getNeighbor(dir);
				if (node.getGame().getBoard().isPositionOccupied(neighbor)) {
					Piece piece = node.getGame().getBoard().getPieceAtPosition(neighbor);
					if (!king.isEnemyPiece(piece)) {
						shieldValue += Piece.getPointValue(piece.getType());
					}
				}
			}
			return shieldValue;
		}
		
		
		
	}

	public static double getOffensiveMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		// remember the action has already taken affect at this point, so capture moves have already resolved
		// and the targeted piece will not exist inside the game anymore.
		// however this value was recorded in the amount of points that the player has earned in this node
		double damageDealtInThisNode = node.getGame().getBoard().getPointsEarned(CustomHeuristics.getMaxPlayer(node));

		switch(node.getMove().getType())
		{
		case PROMOTEPAWNMOVE:
			PromotePawnMove promoteMove = (PromotePawnMove)node.getMove();
			damageDealtInThisNode += Piece.getPointValue(promoteMove.getPromotedPieceType());
			break;
		default:
			break;
		}
		// offense can typically include the number of pieces that our pieces are currently threatening
		int numPiecesWeAreThreatening = OffensiveHeuristics.getNumberOfPiecesMaxPlayerIsThreatening(node);

		return damageDealtInThisNode + numPiecesWeAreThreatening;
	}

	public static double getDefensiveMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		// how many pieces exist on our team?
		int numPiecesAlive = DefensiveHeuristics.getNumberOfMaxPlayersAlivePieces(node);

		// what is the state of the pieces next to the king? add up the values of the neighboring pieces
		// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
		int kingSurroundingPiecesValueTotal = DefensiveHeuristics.getClampedPieceValueTotalSurroundingMaxPlayersKing(node);

		// how many pieces are threatening us?
		int numPiecesThreateningUs = DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node);

		return numPiecesAlive + kingSurroundingPiecesValueTotal + numPiecesThreateningUs;
	}

	public static double getNonlinearPieceCombinationMaxPlayerHeuristicValue(DFSTreeNode node)
	{
		// both bishops are worth more together than a single bishop alone
		// same with knights...we want to encourage keeping pairs of elements
		double multiPieceValueTotal = 0.0;

		double exponent = 1.5; // f(numberOfKnights) = (numberOfKnights)^exponent

		// go over all the piece types that have more than one copy in the game (including pawn promotion)
		for(PieceType pieceType : new PieceType[] {PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN})
		{
			multiPieceValueTotal += Math.pow(node.getGame().getNumberOfAlivePieces(CustomHeuristics.getMaxPlayer(node), pieceType), exponent);
		}

		return multiPieceValueTotal;
	}

	public static int getCentralControlValue(DFSTreeNode node) {
		// Calculates the value of controlling central squares on the board
		int centralScore = 0;
		for (Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node))) {
			Coordinate pos = piece.getCurrentPosition(node.getGame().getBoard());
			if (pos.getXPosition() >= 3 && pos.getXPosition() <= 4 && pos.getYPosition() >= 3 && pos.getYPosition() <= 4) {
				centralScore += Piece.getPointValue(piece.getType());
			}
		}
		return centralScore;
	}
	public static int getPieceMobilityValue(DFSTreeNode node) {
		// Calculates how many legal moves the max player's pieces can make.
		int mobilityScore = 0;
		for (Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node))) {
			mobilityScore += piece.getAllMoves(node.getGame()).size();
		}
		return mobilityScore;
	}
	public static int getCaptureThreatValue(DFSTreeNode node) {
		 // Calculates the threat posed by max player's pieces in terms of potential captures.
   
		int captureThreat = 0;
		for (Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node))) {
			captureThreat += piece.getAllCaptureMoves(node.getGame()).size();
		}
		return captureThreat;
	}
	
	
	
	
		
	
	
	

	public static double getMaxPlayerHeuristicValue(DFSTreeNode node) {
		double offenseHeuristicValue = CustomHeuristics.getOffensiveMaxPlayerHeuristicValue(node);
		double defenseHeuristicValue = CustomHeuristics.getDefensiveMaxPlayerHeuristicValue(node);
		double nonlinearHeuristicValue = CustomHeuristics.getNonlinearPieceCombinationMaxPlayerHeuristicValue(node);
		
		// Additional heuristics
		double kingSafety = DefensiveHeuristics.evaluateKingSafety(node);
		double kingShield = DefensiveHeuristics.getKingShieldValue(node);
		double centralControl = getCentralControlValue(node);
		double mobility = getPieceMobilityValue(node);
		double captureThreat = getCaptureThreatValue(node);
		double aggressiveCastling = OffensiveHeuristics.evaluateAggressiveCastling(node);
		double capturePotential = OffensiveHeuristics.getCapturePotential(node);
	
		return offenseHeuristicValue 
			+ defenseHeuristicValue 
			+ nonlinearHeuristicValue 
			+ kingSafety 
			+ kingShield 
			+ centralControl 
			+ mobility 
			+ captureThreat 
			+ aggressiveCastling 
			+ capturePotential;
	}
	
}
