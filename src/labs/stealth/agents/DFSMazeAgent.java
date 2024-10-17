package src.labs.stealth.agents;

// SYSTEM IMPORTS
import edu.bu.labs.stealth.agents.MazeAgent;
import edu.bu.labs.stealth.graph.Vertex;
import edu.bu.labs.stealth.graph.Path;
import edu.cwru.sepia.environment.model.state.State.StateView;

// will need for dfs
// will need for dfs
// will need for dfs
import java.util.*;


// JAVA PROJECT IMPORTS


public class DFSMazeAgent 
    extends MazeAgent 
{

    public DFSMazeAgent(int playerNum) 
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src, Vertex goal, StateView state) 
    {
        Stack<Path> stack = new Stack<>(); 
        Set<Vertex> visitedSet = new HashSet<>(); // visited vertexes/coordinates

        visitedSet.add(src); // add source node to visited
        stack.push(new Path(src, 0f, null)); // push the initial path onto stack

        while (!stack.isEmpty()) {
            Path currentPath = stack.pop();
            Vertex currentVertex = currentPath.getDestination();

            // get neighbors for current vertex
            ArrayList<Vertex> neighbors = getNeighbors(currentVertex, state);

            for (Vertex neighbor : neighbors) {
                if (!visitedSet.contains(neighbor)) {
                    visitedSet.add(neighbor); 
                    Path newPath = new Path(neighbor, 1f, currentPath);
                    
                    for (Vertex v : getNeighbors(goal, state)) {
                        if (v.equals(neighbor)) {
                            return newPath;
                        }
                    }
                    stack.push(newPath); // push new path onto stack
                }
            }
        }

        return null; 
    }

    // return arraylist that contains valid neighbors of given vertex v
    public ArrayList<Vertex> getNeighbors(Vertex v, StateView state) 
    {
        ArrayList<Vertex> neighbors = new ArrayList<>();
        int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, 
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1} 
        };
    
        for (int[] direction : DIRECTIONS) {
            int newX = v.getXCoordinate() + direction[0];
            int newY = v.getYCoordinate() + direction[1];
            
            // check if the new coordinates are within bounds and not blocked
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
