package com.example.abm;

import lombok.Getter;
import lombok.Setter;
import org.simreal.annotation.*;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.IntBag;

import java.util.ArrayList;
import java.util.stream.Collectors;

@SimModel()
@Getter
@Setter
public class Model extends SimState  {
	public static final long serialVersionUID = 1L;
	private int population;
	private Bag field;
	public Model(@SimParam(value = "50") int population,
				 @SimParam(value = "50") int wealth) {
		super(System.currentTimeMillis());
		this.field = new Bag();
		this.population = population;
		// create agents
		createAgents(wealth);
	}
	private void createAgents(int wealth)
	{
		// clear model field
		field.clear();
		// create agents
		for(int i=0; i <population; i++)
		{
			Agent temp_agt = new Agent(this);
			temp_agt.setWealth(wealth);
			schedule.scheduleRepeating(temp_agt);
			// add agent to model field
			field.add(temp_agt);
		}
	}
	@SimChart(name="Top10 Wealth")
	public int lineChart()
	{
		// sort the bag of the population
		IntBag popln_wealth = new IntBag();
		// get the wealth in a intBag and sort it
		((ArrayList<Agent>) field.stream().collect(Collectors.toList())).forEach((agt_arg) -> {
			Agent agt = agt_arg;
			popln_wealth.add(agt.getWealth());
		});
		popln_wealth.sort();
		// get the sum of the top 10% wealth
		int top10_wealth_sum = 0;
		int top10_sz = (int) (population * 0.1);
		for(int i=population-top10_sz; i<population; i++)
		{
			top10_wealth_sum += popln_wealth.get(i);
		}
		return top10_wealth_sum;
	}

	@SimDB(name = "agents_data")
	public ArrayList<Agent> agentsToDB()
	{
		return (ArrayList<Agent>) field.stream().collect(Collectors.toList());
	}

	public static void main(String[] args) {
		Model model_obj = new Model(50, 50);
		do {
			boolean is_step = model_obj.schedule.step(model_obj);
			if(!is_step) {
				break;
			}
			System.out.println("tick = " + model_obj.schedule.getSteps());
		}
		while(model_obj.schedule.getSteps() < 100);
	}
}