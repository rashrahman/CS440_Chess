package src.labs.infexf.agents;

// SYSTEM IMPORTS
import edu.bu.labs.infexf.agents.SpecOpsAgent;
import edu.bu.labs.infexf.distance.DistanceMetric;
import edu.bu.labs.infexf.graph.Vertex;
import edu.bu.labs.infexf.graph.Path;


import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.ArrayList;
import java.util.List;


// JAVA PROJECT IMPORTS


public class InfilExfilAgent
    extends SpecOpsAgent
{

    public InfilExfilAgent(int playerNum)
    {
        super(playerNum);
    }

    // if you want to get attack-radius of an enemy, you can do so through the enemy unit's UnitView
    // Every unit is constructed from an xml schema for that unit's type.
    // We can lookup the "range" of the unit using the following line of code (assuming we know the id):
    //     int attackRadius = state.getUnit(enemyUnitID).getTemplateView().getRange();
    @Override
    public float getEdgeWeight(Vertex src,
                               Vertex dst,
                               StateView state)
    {
        float distance = DistanceMetric.chebyshevDistance(src, dst);

        // Base weight
        float edgeWeight = Math.max(distance, 1f);



        // for each enemy
        for (Integer enemyID : getOtherEnemyUnitIDs()) {
            // enemy position
            UnitView enemyUnit = state.getUnit(enemyID);
            int enemyX = enemyUnit.getXPosition();
            int enemyY = enemyUnit.getYPosition();
            Vertex enemyVertex = new Vertex(enemyX, enemyY);
            
            // distance is close to enemy
            if (DistanceMetric.chebyshevDistance(dst, enemyVertex) <= 3) {
                edgeWeight += 1000f;
                return edgeWeight;  // Add high cost to avoid this path
            }
        }

        // If no enemy danger is detected:
       
        edgeWeight += distance;

        return edgeWeight;

    }


    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        // Loop through each step in the current plan
        for (Vertex step : getCurrentPlan()) {
            // if path is blocked by a resource, replace the plan
            if (state.isResourceAt(step.getXCoordinate(), step.getYCoordinate())) {
                return true;
            }
            // for each enemy
            for (Integer enemyID : getOtherEnemyUnitIDs()) {
                // enemy position
                UnitView enemyUnit = state.getUnit(enemyID);
                if (enemyUnit == null){
                    continue;
                }
                int enemyX = enemyUnit.getXPosition();
                int enemyY = enemyUnit.getYPosition();
                Vertex enemyVertex = new Vertex(enemyX, enemyY);
                
                // if enemy too close, replace plan
                if (DistanceMetric.chebyshevDistance(step, enemyVertex) <= 3) {
                    return true;
                }
            }
            
        }

        return false;
    }

}
