package com.example.abm;

import java.lang.String;
import lombok.Getter;
import lombok.Setter;
import org.simreal.annotation.SimAgent;
import org.simreal.annotation.SimField;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Int2D;

@SimAgent
@Getter
@Setter
public class Agent implements Steppable {
  public static final long serialVersionUID = 1L;

  public Life model;

  @SimField
  public String agent_id;

  // define member variables
  @SimField
  private boolean alive = false;

  public Agent(Life model) {
    this.model = model;
    this.agent_id = String.valueOf(model.random.nextChar()) + model.random.nextInt(10000000);
  }

  public void step(SimState sim_state) {
    // perform agent actions here
    // get model from argument
    // LifeModel lifeModel = (LifeModel) simState;
    // extract the grid from model
    SparseGrid2D worldSparse = model.getLife_world();
    // get current cell agent location
    Int2D cell_loc = worldSparse.getObjectLocation(this);
    // get neighbouring cells from grid (8 neighbors, Moore neighbours)
    Bag neighbours = worldSparse.getMooreNeighbors(cell_loc.getX(), cell_loc.getY(), 1, SparseGrid2D.TOROIDAL, false);
    // check condition of neighbors and take action
    cellActions(neighbours);
  }

  // a helper method to determine if a cell should be alive or dead
  private void cellActions(Bag nbrs){
    int live_nbrs = 0;
    // count the number of alive cells
    for(Object one_nbr : nbrs)
    {
      Agent nbr_cell = (Agent) one_nbr;
      // if neighbor is alive then increment count of alive neighbors
      if(nbr_cell.isAlive())
      {
        live_nbrs++;
      }
    }

    // check the count of live neighbors and determine the status of cell accordingly
    if(live_nbrs == 3)
    {
      // if the cell has 3 alive neighbors, then it lives (even if its dead or alive)
      this.setAlive(true);
    }
    else if(live_nbrs < 2 || live_nbrs >= 4)
    {
      // if the cell has less than 2 alive neighbors or more than 3 alive neighbors, it dies
      this.setAlive(false);
    }
    // the condition of two alive cells is left not coded because in that case the cell is left as it is

    // ** end of cellActions method
  }

  // getters and setters
  public boolean isAlive()
  {
    return alive;
  }

  public boolean getAlive() {
    return alive;
  }

  public void setAlive(boolean alive)
  {
    this.alive = alive;
  }
}
