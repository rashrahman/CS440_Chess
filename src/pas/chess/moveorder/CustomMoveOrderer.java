package src.pas.chess.moveorder;


// SYSTEM IMPORTS
import edu.bu.chess.search.DFSTreeNode;

import java.util.LinkedList;
import java.util.List;


// JAVA PROJECT IMPORTS
import src.pas.chess.moveorder.DefaultMoveOrderer;

public class CustomMoveOrderer
    extends Object
{

	/**
	 * TODO: implement me!
	 * This method should perform move ordering. Remember, move ordering is how alpha-beta pruning gets part of its power from.
	 * You want to see nodes which are beneficial FIRST so you can prune as much as possible during the search (i.e. be faster)
	 * @param nodes. The nodes to order (these are children of a DFSTreeNode) that we are about to consider in the search.
	 * @return The ordered nodes.
	 */
	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes)
	{
		List<DFSTreeNode> captureNodes = new LinkedList<>();
        List<DFSTreeNode> promotionNodes = new LinkedList<>();
        List<DFSTreeNode> castlingNodes = new LinkedList<>();
        List<DFSTreeNode> otherNodes = new LinkedList<>();

        for (DFSTreeNode node : nodes) {
            if (node.getMove() != null) {
                switch (node.getMove().getType()) {
                    case CAPTUREMOVE:
                        captureNodes.add(node);
                        break;
                    case PROMOTEPAWNMOVE:
                        promotionNodes.add(node);
                        break;
                    case CASTLEMOVE:
                        castlingNodes.add(node);
                        break;
                    default:
                        otherNodes.add(node);
                        break;
                }
            } else {
                otherNodes.add(node);
            }
        }

        // Combine in the order: captures first, then promotions, castling, then other moves
        List<DFSTreeNode> orderedNodes = new LinkedList<>();
        orderedNodes.addAll(captureNodes);
        orderedNodes.addAll(promotionNodes);
        orderedNodes.addAll(castlingNodes);
        orderedNodes.addAll(otherNodes);

        return orderedNodes;
	}

}
