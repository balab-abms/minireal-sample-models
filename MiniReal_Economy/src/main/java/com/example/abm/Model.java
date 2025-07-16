package com.example.abm;

import lombok.Getter;
import lombok.Setter;
import org.simreal.annotation.*;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.IntBag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SimModel
@Getter
@Setter
public class Model extends SimState  {
	public static final long serialVersionUID = 1L;
	private int population;
	private Bag field;
	public Model(@SimParam(value = "500") int population,
				 @SimParam(value = "100") int wealth) {
		super(System.currentTimeMillis());
		this.field = new Bag();
		this.population = population;
		createAgents(wealth);
	}
	private void createAgents(int wealth){
		// clear model field and create agents
		field.clear();
		for(int i=0; i <population; i++){
			Agent temp_agt = new Agent(this);
			temp_agt.setWealth(wealth);
			schedule.scheduleRepeating(temp_agt);
			// add agent to model field
			field.add(temp_agt);
		}
	}
//	@SimChart(name="top10")
	public int top10wealth(){
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
		for(int i=population-top10_sz; i<population; i++){	top10_wealth_sum += popln_wealth.get(i);  }
		return top10_wealth_sum;
	}
//	@SimChart(name="bottom50")
	public int bottom50wealth()	{
		// sort the bag of the population
		IntBag popln_wealth = new IntBag();
		// get the wealth in a intBag and sort it
		((ArrayList<Agent>) field.stream().collect(Collectors.toList())).forEach((agt_arg) -> {
			Agent agt = agt_arg;
			popln_wealth.add(agt.getWealth());
		});
		popln_wealth.sort();
		// get the sum of the bootom 50% wealth
		int bottom50_wealth_sum = 0;
		int bottom50_sz = (int) (population * 0.5);
		for(int i=0; i<bottom50_sz; i++){	bottom50_wealth_sum += popln_wealth.get(i);	 }
		return bottom50_wealth_sum;
	}

	@SimChart(name = "Gini")
	public double computeGini() {
		// Extract all agent wealths into a List<Double>
		List<Double> wealths = ((ArrayList<Agent>) field.stream().collect(Collectors.toList()))
				.stream()
				.map(Agent::getWealth)
				.map(Integer::doubleValue)
				.collect(Collectors.toList());

		final int N = wealths.size();
		if (N == 0) return 0;

		// Sort ascending
		Collections.sort(wealths);

		// Compute B = ∑_{i=1}^N x_i * (N - i + 1) / (N * ∑ x)
		double sumX = wealths.stream().mapToDouble(Double::doubleValue).sum();
		double B = 0;
		for (int i = 0; i < N; i++) {
			// `i` is zero‐based: position (i+1)
			B += wealths.get(i) * (N - i);
		}
		B = B / (N * sumX);

		// 4) Gini = 1 + 1/N - 2B
		return 1.0 + (1.0 / N) - 2.0 * B;
	}

	public static void main(String[] args) {
		Model model_obj = new Model(50, 50);
		do {
			boolean is_step = model_obj.schedule.step(model_obj);
			if(!is_step) {	break;	}
			System.out.println("tick = " + model_obj.schedule.getSteps());
		} while(model_obj.schedule.getSteps() < 100);
	}
}

