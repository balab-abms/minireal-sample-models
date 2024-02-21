package com.example.application;

import lombok.Getter;
import lombok.Setter;
import org.simreal.annotation.*;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.IntBag;

import java.util.ArrayList;

@SimModel()
@Getter
@Setter
public class Model extends SimState  {
	public static final long serialVersionUID = 1L;
	private int population;
	private Bag field;
	private ArrayList<Agent> agents_list;
	public Model(@SimParam(value = "50") int population,
				 @SimParam(value = "100") int wealth) {
		super(System.currentTimeMillis());
		this.field = new Bag();
		this.agents_list = new ArrayList<>();
		this.population = population;
		// create agents
		createAgents(population, wealth);
	}
	private void createAgents(int popln, int wealth)
	{
		// insert code to be executed at the start of the model
		field.clear();
		// create agents
		for(int i=0; i <popln; i++)
		{
			Agent temp_agt = new Agent(this);
			temp_agt.setWealth(wealth);
			field.add(temp_agt);
			schedule.scheduleRepeating(temp_agt);
			agents_list.add(temp_agt);
		}
	}
	@SimChart(name="Top10 Wealth")
	public int lineChart()
	{
		int popln_sz = agents_list.size();
		// sort the bag of the population
		IntBag popln_wealth = new IntBag();
		// get the wealth in a intBag and sort it
		agents_list.forEach((agt_arg) -> {
			Agent agt = agt_arg;
			popln_wealth.add(agt.getWealth());
		});
		popln_wealth.sort();
		// get the sum of the top 10% wealth
		int top10_wealth_sum = 0;
		int top10_sz = (int) (popln_sz * 0.1);
		for(int i=popln_sz-top10_sz; i<popln_sz; i++)
		{
			top10_wealth_sum += popln_wealth.get(i);
		}
		return top10_wealth_sum;
	}

	@SimDB(name = "agents_data")
	public ArrayList<Agent> agentsToDB()
	{
		return agents_list;

	}

	@SimModelVisual()
	public ArrayList<Agent> toVisual()
	{
		return agents_list;

	}
}