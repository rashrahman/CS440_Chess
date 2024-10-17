package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;

// will need for bfs
// will need for bfs
// will need for bfs
// will need for bfs
import java.util.*;


// JAVA PROJECT IMPORTS


public class BFSMazeAgent
    extends MazeAgent
{

    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        Queue<Path> q = new LinkedList<>(); // path taken by BFS
        Set<Vertex> visitedSet = new HashSet<>(); // visited vertexes/coordinates

        visitedSet.add(src); // add source node to visited
        q.offer(new Path(src, 0f, null)); // enqueue, no "actual" path yet

        while (!q.isEmpty()) {
            Path currentPath = q.poll();
            Vertex currentVertex = currentPath.getDestination();

            // get neighbors for current vertex
            ArrayList<Vertex> neighbors = new ArrayList<Vertex>();
            neighbors = getNeighbors(currentVertex, state);

            for (Vertex neighbor : neighbors) {
                if (!visitedSet.contains(neighbor)) {
                    visitedSet.add(neighbor); 
                    Path newPath = new Path(neighbor, 1f, currentPath);
    
                    for (Vertex v : getNeighbors(goal, state)) {
                        if (v.equals(neighbor)) {
                            return newPath;
                        }
                    }
                    q.offer(newPath);  // Add the path to the queue
                }
            }
        }
        return null;
    }

    // return arraylist that contains valid neighbors of given vertex v
    public ArrayList<Vertex> getNeighbors(Vertex v, StateView state) {
        ArrayList<Vertex> neighbors = new ArrayList<>();
        int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, 
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1} 
        };
    
        for (int[] direction : DIRECTIONS) {
            int newX = v.getXCoordinate() + direction[0];
            int newY = v.getYCoordinate() + direction[1];
            
            // Check if the new coordinates are within bounds and not blocked
            if (state.inBounds(newX, newY) && !state.isResourceAt(newX, newY)) {
                neighbors.add(new Vertex(newX, newY));
            }
        }
        return neighbors;
    }
    

    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        return false;
    }


}