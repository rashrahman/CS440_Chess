package src.pas.chess.moveorder;


// SYSTEM IMPORTS
import edu.bu.chess.search.DFSTreeNode;

import java.util.LinkedList;
import java.util.List;


// JAVA PROJECT IMPORTS


public class DefaultMoveOrderer
    extends Object
{

	/**
	 * By default, I claim that we want to see attacking moves before anything else. However,
	 * this is not a good rule in general, and we may want to make it move-specific OR start incorporating some custom heuristics
	 * @param nodes. The nodes to order (these are children of a DFSTreeNode) that we are about to consider in the search.
	 * @return The ordered nodes.
	 */
	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes)
	{
		// by default get the CaptureMoves first
		List<DFSTreeNode> captureNodes = new LinkedList<DFSTreeNode>();
		List<DFSTreeNode> otherNodes = new LinkedList<DFSTreeNode>();

		for(DFSTreeNode node : nodes)
		{
			if(node.getMove() != null)
			{
				switch(node.getMove().getType())
				{
				case CAPTUREMOVE:
					captureNodes.add(node);
					break;
				default:
					otherNodes.add(node);
					break;
				}
			} else
			{
				otherNodes.add(node);
			}
		}

		captureNodes.addAll(otherNodes);
		return captureNodes;
	}

}
