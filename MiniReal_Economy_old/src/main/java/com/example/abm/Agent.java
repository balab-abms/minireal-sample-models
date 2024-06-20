package com.example.abm;

import lombok.Getter;
import lombok.Setter;
import org.simreal.annotation.SimAgent;
import org.simreal.annotation.SimField;
import sim.engine.Steppable;
import sim.engine.SimState;
import sim.util.Bag;

@SimAgent
@Getter
@Setter
public class Agent implements Steppable {

	public static final long serialVersionUID = 1L;
	public Model model;
	@SimField
	private String agent_id;
	@SimField
	private int wealth;

	@SimField
	private long step;
	public Agent(Model model) {
		this.model = model;
		this.agent_id = String.valueOf(model.random.nextInt(1000000));
	}

	public void step(SimState simState) {
		this.step = model.schedule.getSteps();
		// extract bag from model
		Bag economySpace = model.getField();
		// perform transaction
		transact(economySpace);
	}
	private void transact(Bag popln)
	{
		// get a random agent from the population
		Agent counterAgent = (Agent) popln.get(model.random.nextInt(popln.size()));
		// transact wealth from current agent to random_agent
		if(this.wealth > 0)
		{
			this.wealth -= 1;
			counterAgent.addWealth();
		}
	}
	// helper methods
	public void addWealth()
	{
		this.wealth +=1;
	}
}