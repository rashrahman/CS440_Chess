package src.pas.chess.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.bu.chess.agents.ChessAgent;
import edu.bu.chess.game.Game;
import edu.bu.chess.game.move.Move;
import edu.bu.chess.game.planning.Planner;
import edu.bu.chess.game.player.Player;
import edu.bu.chess.game.player.PlayerType;
import edu.bu.chess.search.DFSTreeNode;
import edu.bu.chess.search.DFSTreeNodeType;
import edu.bu.chess.streaming.Streamer;
import edu.bu.chess.utils.Pair;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


// JAVA PROJECT IMPORTS
import src.pas.chess.heuristics.CustomHeuristics;


public class MinimaxAgent
    extends ChessAgent
{

	private class MinimaxSearcher
        extends Object
        implements Callable<Pair<Move, Long> >  // so this object can be run in a background thread
	{

		private DFSTreeNode rootNode;
		private final int maxDepth;

		public MinimaxSearcher(DFSTreeNode rootNode, int maxDepth)
		{
			this.rootNode = rootNode;
			this.maxDepth = maxDepth;
		}

		public DFSTreeNode getRootNode() { return this.rootNode; }
		public int getMaxDepth() { return this.maxDepth; }

		public DFSTreeNode minimaxSearch(DFSTreeNode node, int depth)
		{
			DFSTreeNode bestChild = null;
			if(node.isTerminal()) // terminal state!
			{
				bestChild = node;
			} else if(depth <= 0) // reached the end of the depth!
			{
				// assign heuristic value to the child as its utility
				node.setMaxPlayerUtilityValue(CustomHeuristics.getMaxPlayerHeuristicValue(node));
				bestChild = node;
			} else // we can get the children of this node and find its best value
			{
				List<DFSTreeNode> children = node.getChildren();

				double bestUtilityValue;
				if(node.getType() == DFSTreeNodeType.MAX)
				{
					bestUtilityValue = Double.NEGATIVE_INFINITY;
					for(DFSTreeNode child : children)
					{
						child.setMaxPlayerUtilityValue(this.minimaxSearch(child, depth-1).getMaxPlayerUtilityValue());
						if(child.getMaxPlayerUtilityValue() > bestUtilityValue)
						{
							bestUtilityValue = child.getMaxPlayerUtilityValue();
							bestChild = child;
						}
					}
				} else
				{
					// min
					bestUtilityValue = Double.POSITIVE_INFINITY;
					for(DFSTreeNode child : children)
					{
						child.setMaxPlayerUtilityValue(this.minimaxSearch(child, depth-1).getMaxPlayerUtilityValue());
						if(child.getMaxPlayerUtilityValue() < bestUtilityValue)
						{
							bestUtilityValue = child.getMaxPlayerUtilityValue();
							bestChild = child;
						}
					}
				}
			}
			return bestChild;
		}

		@Override
		public Pair<Move, Long> call() throws Exception
		{
			Move move = null;

			double startTime = System.nanoTime();
			move = this.minimaxSearch(this.getRootNode(), this.getMaxDepth()).getMove();
			double endTime = System.nanoTime();

			return new Pair<Move, Long>(move, (long)((endTime-startTime)/1000000));
		}
		
	}

	private static final long serialVersionUID = -8325987205183244708L;
	private final int maxDepth;
	private final long maxPlaytimeInMS;
	private final PlayerType playerType;

	private Player myPlayer;

	/**
	 * The constructor. Please do not modify. This constructor will work for variable-sized program args
	 * @param playerID
	 * @param args
	 */
	public MinimaxAgent(int playerID, String[] args)
	{
		super(playerID);
		long maxPlaytimeInMS = 0;
		int maxDepth = 10;
		String playerTypeString = null;
		String filePath = null;

		if(args.length == 3)
		{
			playerTypeString = args[1];
			maxPlaytimeInMS = Long.parseLong(args[2]) * 1000;
		} else if (args.length == 4)
		{
			playerTypeString = args[1];
			maxPlaytimeInMS = Long.parseLong(args[2]) * 1000;
			maxDepth = Integer.parseInt(args[3]);
		} else if(args.length == 5)
		{
			playerTypeString = args[1];
			maxPlaytimeInMS = Long.parseLong(args[2]) * 1000;
			maxDepth = Integer.parseInt(args[3]);
			filePath = args[4];
		} else
		{
			System.err.println("MinimaxAgent.MinimaxAgent [ERROR]: not enough arguments. Must specify player type, total playing time (in seconds), (optionally) maxdepth, and (optionally) a filepath");
			System.exit(-1);
		}

		this.playerType = PlayerType.valueOf(playerTypeString);
		this.maxDepth = maxDepth;
		this.maxPlaytimeInMS = maxPlaytimeInMS;
		this.myPlayer = null;
		this.setFilePath(filePath);

		System.out.println("Constructed MinimaxAgent(teamColor=" + this.getPlayerType() + ", timeLimit(ms)=" + this.getMaxPlaytimeInMS() + ", maxDepth=" + this.getMaxDepth() + ")");
	}

	/**
	 * Some constants
	 */
	public int getMaxDepth() { return this.maxDepth; }
	public long getMaxPlaytimeInMS() { return this.maxPlaytimeInMS; }

	@Override
	public PlayerType getPlayerType() { return this.playerType; }

	@Override
	protected Player getPlayer() { return this.myPlayer; }

	/**
	 * This method is responsible for getting a chess move selected via the minimax algorithm.
	 * There is some setup for this to work, namely making sure the agent doesn't run out of time.
	 * Please do not modify.
	 */
	@Override
	protected Move getChessMove(StateView state)
	{
        Planner.getPlanner().freeze(this.getPlayer());

		// will run the minimax algorithm in a background thread with a timeout
		ExecutorService backgroundThreadManager = Executors.newSingleThreadExecutor();

		// preallocate so we don't spend precious time doing it when we are recording duration
		Move move = null;
		long durationInMs = 0;
		DFSTreeNode rootNode = new DFSTreeNode(Planner.getPlanner().getGame(), this.getPlayer());
		MinimaxSearcher searcherObject = new MinimaxSearcher(rootNode, this.getMaxDepth()); // this obj will run in the background

		// submit the job
		Future<Pair<Move, Long> > future = backgroundThreadManager.submit(searcherObject);

		try
		{
			// set the timeout
			Pair<Move, Long> moveAndDuration = future.get(Planner.getPlanner().getGame().getTimeLeftInMS(this.getPlayer()),
					TimeUnit.MILLISECONDS);

			// if we get here the move was chosen quick enough! :)
			move = moveAndDuration.getFirst();
			durationInMs = moveAndDuration.getSecond();

			// convert the move into a text form (algebraic notation) and stream it somewhere
			Streamer.getStreamer(this.getFilePath()).streamMove(move, Planner.getPlanner().getGame());
		} catch(TimeoutException e)
		{
			// timeout = out of time...get ready to end the game (by subtracting all of the time we had left)
			durationInMs = this.getMaxPlaytimeInMS();
		} catch(InterruptedException e)
		{
			e.printStackTrace();
			System.exit(-1);
		} catch(ExecutionException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		// update the game singleton to record that our player took some time to think
		Planner.getPlanner().getGame().removeTimeFromPlayer(this.getPlayer(), durationInMs); // convert duration to ms

        Planner.getPlanner().thaw(this.getPlayer());

		return move;
	}

	/**
	 * The initial step which we use for setup. Please do not modify.
	 */
	@Override
	public Map<Integer, Action> initialStep(StateView state, HistoryView history)
	{
		// register the player with the game
		Game game = Planner.getPlanner().getGame(state, this.getMaxPlaytimeInMS());
		game.registerPlayer(this.getPlayerNumber(), this.getPlayerType(), state);

		// remember what player we are
		this.myPlayer = game.getPlayer(this.getPlayerType());

		// init streamer
		Streamer.getStreamer(this.getFilePath());
		return null;
	}

	@Override
	public void loadPlayerData(InputStream stream)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * This is the middlestep. Here we only do something if it is our turn to play (synchronized by a singleton).
	 * When it is our turn, we can either end the game (by killing all of our remaining pieces) if we are in a terminal state,
	 * OR have to deal with an action.
	 * 
	 * Chess moves boil down into multiple SEPIA actions, so we only want to generate a new chess move IFF all of the SEPIA actions
	 * from the previous move have completed (or if there was no previous move). This state machine is controlled by a Planner singleton
	 * to keep everying in one place. We can either submit a new chess move (which we spend time to calculate) to the planner OR
	 * grab the current SEPIA action from the planner.
	 */
	@Override
	public Map<Integer, Action> middleStep(StateView state, HistoryView history)
	{
		Map<Integer, Action> actions = new HashMap<Integer, Action>();

		if(Planner.getPlanner().isMyTurn(this.getPlayerType())) // only allow the white player to move for now
		{
			if(Planner.getPlanner().isGameOver())
			{
				actions = this.killMyPieces(state);
			}
			else
			{
				// System.out.println("MinimaxAgent.middleStep [INFO] game is not over!");
				if(Planner.getPlanner().canSubmitMove())
				{
					Move move = this.getChessMove(state);
					// System.out.println("MinimaxAgent.middleStep [INFO] selected move=" + move);
		
					// System.out.println("MinimaxAgent.middleStep [INFO] getPlanner().canSubmitMove()=" + Planner.getPlanner().canSubmitMove());
					if(Planner.getPlanner().canSubmitMove())
					{
						Planner.getPlanner().submitMove(move, Planner.getPlanner().getGame());
					}
				}

				Action action = Planner.getPlanner().getAction(this.getPlayer(), state);
				// System.out.println("MinimaxAgent.middleStep [INFO] current action=" + action);
				if(action != null)
				{
					// do something
					actions.put(action.getUnitId(), action);
				}
			}
		}
		return actions;
	}

	@Override
	public void savePlayerData(OutputStream history)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void terminalStep(StateView state, HistoryView history)
	{
		// TODO Auto-generated method stub

	}

}
