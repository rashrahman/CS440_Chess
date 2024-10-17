package src.pas.chess.debug.agents;


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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


// JAVA PROJECT IMPORTS
import src.pas.chess.heuristics.CustomHeuristics;
import src.pas.chess.agents.AlphaBetaAgent;


public class MinimaxReflectionAgent
    extends ChessAgent
{
    private class AlphaBetaReflectionSearcher
        extends Object
        implements Callable<Pair<Move, Double> >  // so this object can be run in a background thread
	{

		private DFSTreeNode rootNode;
		private final int maxDepth;
        private final Object searcher;
        private final Method alphaBetaSearchMethod;

		public AlphaBetaReflectionSearcher(DFSTreeNode rootNode,
                                           int maxDepth,
                                           AlphaBetaAgent agent,
                                           Constructor<?> c,
                                           Method m)
		{
			this.rootNode = rootNode;
			this.maxDepth = maxDepth;
            Object searcher = null;
            try
            {
                searcher = c.newInstance(agent, rootNode, maxDepth);
            } catch(Exception e)
            {
                e.printStackTrace();
                System.exit(-1);
            }
            this.searcher = searcher;
            this.alphaBetaSearchMethod = m;
		}

		public DFSTreeNode getRootNode() { return this.rootNode; }
		public int getMaxDepth() { return this.maxDepth; }
        public Object getSearcher() { return this.searcher; }
        public Method getSearchMethod() { return this.alphaBetaSearchMethod; }

		@Override
		public Pair<Move, Double> call() throws Exception
		{
			DFSTreeNode n = (DFSTreeNode)this.getSearchMethod().invoke(this.getSearcher(),
                this.getRootNode(), this.getMaxDepth(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

			return new Pair<Move, Double>(n.getMove(), n.getMaxPlayerUtilityValue());
		}
		
	}


	private class MinimaxSearcher
        extends Object
        implements Callable<Pair<Move, Double> >  // so this object can be run in a background thread
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
		public Pair<Move, Double> call() throws Exception
		{
			DFSTreeNode n = this.minimaxSearch(this.getRootNode(), this.getMaxDepth());

			return new Pair<Move, Double>(n.getMove(), n.getMaxPlayerUtilityValue());
		}
		
	}

	private static final long serialVersionUID = -8325987205183244708L;
    private static final int MAX_NUM_MOVES_BEFORE_QUIT = 10;
	private final int maxDepth;
	private final long maxPlaytimeInMS;
	private final PlayerType playerType;

	private Player myPlayer;

    private final AlphaBetaAgent      alphaBetaAgent;
    private final Class<?>            alphaBetaSearcherClassType;
    private final Constructor<?>      alphaBetaSearcherConstructor;
    private final Method              alphaBetaSearchMethod;

    private int numMoves;
    private int numDifferentUtilityMoves;

	/**
	 * The constructor. Please do not modify. This constructor will work for variable-sized program args
	 * @param playerID
	 * @param args
	 */
	public MinimaxReflectionAgent(int playerID, String[] args)
	{
		super(playerID);
		this.alphaBetaAgent = new AlphaBetaAgent(playerID, args);

        this.maxDepth = this.getAlphaBetaAgent().getMaxDepth();
        this.maxPlaytimeInMS = this.getAlphaBetaAgent().getMaxPlaytimeInMS();
        this.playerType = this.getAlphaBetaAgent().getPlayerType();

        // have to use reflection at this point
        Class<?> alphaBetaSearcherClass = null;
        Constructor<?> alphaBetaSearcherConstructor = null;
        Method alphaBetaSearchMethod = null;
        try
        {
            Method getPlayerMethod = null;
            for(Method m : AlphaBetaAgent.class.getDeclaredMethods())
            {
                if(getPlayerMethod == null && m.getName().equals("getPlayer"))
                {
                    getPlayerMethod = m;
                }
            }

            // System.out.println(getPlayerMethod);
            getPlayerMethod.setAccessible(true);
            this.myPlayer = (Player)getPlayerMethod.invoke(this.getAlphaBetaAgent());


            // reflection to find AlphaBetaSearcherStuff
            alphaBetaSearcherClass = Class.forName("src.pas.chess.agents.AlphaBetaAgent$AlphaBetaSearcher");
            // System.out.println(alphaBetaSearcherClass);

            alphaBetaSearcherConstructor =  alphaBetaSearcherClass.getDeclaredConstructors()[0];
            // System.out.println(this.getAlphaBetaSearcherConstructor());

            for(Method m : alphaBetaSearcherClass.getDeclaredMethods())
            {
                if(alphaBetaSearchMethod == null && m.getName().equals("alphaBetaSearch"))
                {
                    alphaBetaSearchMethod = m;
                }
            }

        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        this.alphaBetaSearcherClassType = alphaBetaSearcherClass;
        this.alphaBetaSearcherConstructor = alphaBetaSearcherConstructor;
        this.alphaBetaSearchMethod = alphaBetaSearchMethod;

        this.getAlphaBetaSearcherConstructor().setAccessible(true);
        this.getAlphaBetaSearchMethod().setAccessible(true);

        this.numMoves = 0;
        this.numDifferentUtilityMoves = 0;

		System.out.println("Constructed MinimaxReflectionAgent(teamColor=" + this.getPlayerType() +
            ", timeLimit(ms)=" + this.getMaxPlaytimeInMS() + ", maxDepth=" + this.getMaxDepth() + ")");
	}

	/**
	 * Some constants
	 */
    public AlphaBetaAgent getAlphaBetaAgent() { return this.alphaBetaAgent; }
    public Class<?> getAlphaBetaSearcherClassType() { return this.alphaBetaSearcherClassType; }
    public Constructor<?> getAlphaBetaSearcherConstructor() { return this.alphaBetaSearcherConstructor; }
    public Method getAlphaBetaSearchMethod() { return this.alphaBetaSearchMethod; }

    public int getNumMoves() { return this.numMoves; }
    public int getNumDifferentUtilityMoves() { return this.numDifferentUtilityMoves; }

    public void incNumMoves() { this.numMoves += 1; }
    public void incNumDifferentUtilityMoves() { this.numDifferentUtilityMoves += 1; }

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
		DFSTreeNode rootNode = new DFSTreeNode(Planner.getPlanner().getGame(), this.getPlayer());
		MinimaxSearcher minimaxSearcherObject = new MinimaxSearcher(rootNode, this.getMaxDepth());
        AlphaBetaReflectionSearcher alphaBetaSearcherObject = new AlphaBetaReflectionSearcher(rootNode,
            this.getMaxDepth(),
            this.getAlphaBetaAgent(),
            this.getAlphaBetaSearcherConstructor(),
            this.getAlphaBetaSearchMethod()
        );

		// submit the job
		Future<Pair<Move, Double> > minimaxFuture = backgroundThreadManager.submit(minimaxSearcherObject);
        Future<Pair<Move, Double> > alphaBetaFuture = backgroundThreadManager.submit(alphaBetaSearcherObject);
        

		try
		{
			// set the timeout
			Pair<Move, Double> minimaxMoveAndUtil = minimaxFuture.get(
                    Planner.getPlanner().getGame().getTimeLeftInMS(this.getPlayer()),
					TimeUnit.MILLISECONDS
            );
            Pair<Move, Double> alphaBetaMoveAndUtil = alphaBetaFuture.get(
                    Planner.getPlanner().getGame().getTimeLeftInMS(this.getPlayer()),
					TimeUnit.MILLISECONDS
            );

			// if we get here the move was chosen quick enough! :)
			move = minimaxMoveAndUtil.getFirst();
            this.incNumMoves();
            if(Math.abs(minimaxMoveAndUtil.getSecond() - alphaBetaMoveAndUtil.getSecond()) > 1e-12)
            {
                System.err.println("WARNING: alphabeta and minimax produced different utilities!");
                this.incNumDifferentUtilityMoves();
            }

			// convert the move into a text form (algebraic notation) and stream it somewhere
			Streamer.getStreamer(this.getFilePath()).streamMove(move, Planner.getPlanner().getGame());
		} catch(TimeoutException e)
		{
			// timeout = out of time...get ready to end the game (by subtracting all of the time we had left)
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
        if(this.getNumMoves() >= MinimaxReflectionAgent.MAX_NUM_MOVES_BEFORE_QUIT)
        {
            this.terminalStep(state, history);
            System.exit(-1);
        }

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
        System.out.println("MinimaxReflectionAgent: numMoves=" + this.getNumMoves() +
            " numDifferentUtilityMoves=" + this.getNumDifferentUtilityMoves());
	}

}
