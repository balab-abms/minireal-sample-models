package com.example.abm;

import java.lang.Object;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import org.simreal.annotation.*;
import sim.engine.SimState;
import sim.field.grid.SparseGrid2D;

@SimModel
@Getter
@Setter
public class Life extends SimState {
  public static final long serialVersionUID = 1L;

  private int population;

  // define member variables
  private int world_size = 17;
  public double cells_alive_probability = 0.1;

  // define grid member variables
  private SparseGrid2D life_world;

  public Life(@SimParam(value = "50") int population,
              @SimParam(value = "15") int world_sz) {
    super(System.currentTimeMillis());
    // initialize model environment field
    this.population = population;
    this.world_size = world_sz;
    life_world = new SparseGrid2D(world_size, world_size);
    // clear the grid
    life_world.clear();
    // generate agents
    createAgents(population);
  }

  private void createAgents(int popln) {
    // insert agents generation code here
    // add cell agents to the grid
    for (int i = 0; i < world_size; i++) {
      for (int j = 0; j < world_size; j++) {
        // create a new cell agent
        Agent cell = new Agent(this);
        // randomly set cells 10% of cells to alive (based on probability)
        if (this.random.nextInt(100) < cells_alive_probability * 100) {
          cell.setAlive(true);
        }
        // since cells are created as dead, no action needed for the else clause of the above if statement

        // place the cell object on the grid
        life_world.setObjectLocation(cell, i, j);
        // add the created cell agent to scheduler
        schedule.scheduleRepeating(cell);
      }
    }
  }

    // getters and setters
    public SparseGrid2D getLife_world() {
      return life_world;
    }

  @SimChart(name = "alive_cells"  )
  public int drawChart() {
    // insert chart generation code here
    AtomicInteger alive_cells_num = new AtomicInteger();
    life_world.getAllObjects().stream().forEach(temp_agt -> {
      Agent temp_agt_casted = (Agent) temp_agt;
      if(temp_agt_casted.isAlive())
        alive_cells_num.getAndIncrement();
    });

    return alive_cells_num.get();
  }

  @SimDB(name = "db")
  public ArrayList<Agent> persistAgentsData() {
    // insert agent data persisting code here
    ArrayList<Agent> agents_list = (ArrayList<Agent>) life_world.getAllObjects().stream().collect(Collectors.toList());
    return agents_list;
  }
}
