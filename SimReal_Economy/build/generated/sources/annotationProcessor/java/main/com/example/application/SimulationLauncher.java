package com.example.application;

import com.example.application.kafkaserialize.KafkaTemplateSerializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ClassNotFoundException;
import java.lang.InterruptedException;
import java.lang.Object;
import java.lang.Override;
import java.lang.Runnable;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.Thread;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.kafka.core.KafkaTemplate;

public class SimulationLauncher implements Runnable {
  private KafkaTemplate<String, Object> kafkaTemplate;

  private HashMap<String, Object> sim_params;

  public SimulationLauncher(KafkaTemplate<String, Object> kafkaTemplate,
      HashMap<String, Object> sim_params) {
    this.kafkaTemplate = kafkaTemplate;
    this.sim_params = sim_params;
  }

  @Override
  public void run() {
    // setup simulation and model arguments
    int population = Integer.parseInt(sim_params.containsKey("population") ? sim_params.get("population").toString() : "50");
    int wealth = Integer.parseInt(sim_params.containsKey("wealth") ? sim_params.get("wealth").toString() : "100");
    int steps = Integer.parseInt(sim_params.containsKey("steps") ? sim_params.get("steps").toString() : "100");
    int tick_delay = Integer.parseInt(sim_params.containsKey("tick_delay") ? sim_params.get("tick_delay").toString() : "100");
    Model model = new Model(population, wealth);
    do {
      kafkaTemplate.send("tick", "ui_token", model.schedule.getSteps());
      // send database data;
      sendAgentsData(model);
      // send charting data;
      sendChartingData(model);
      // send visualization data;
      sendVisualData(model);
      boolean is_step = model.schedule.step(model);
      if (!is_step) break;
      try {
        Thread.sleep(tick_delay);
      } catch(InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    while (model.schedule.getSteps() < steps);
  }

  public static void main(String[] args) {
    try {
      // get kafka template from args;
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
      KafkaTemplateSerializer kafkaTemplateSerializer = (KafkaTemplateSerializer) ois.readObject();
      ois.close();
      KafkaTemplate<String, Object> kafkaTemplate_deserialized = kafkaTemplateSerializer.getKafkaTemplate();
      // get model parameters from args;
      Type hash_type = new TypeToken<HashMap<String, Object>>(){}.getType();
      HashMap<String, Object> modelParams_deserialized = args.length > 1 ? new Gson().fromJson(args[1], hash_type) : new HashMap<>();
      System.out.println(modelParams_deserialized.toString());
      // run simulation;
      Runnable model_obj = new SimulationLauncher(kafkaTemplate_deserialized, modelParams_deserialized);
      Thread model_thread = new Thread(model_obj);
      model_thread.start();
    } catch(IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, Object> getAgentData(Model model, Agent agent) {
    Map<String, Object> sim_data = new LinkedHashMap<>();
    sim_data.put("agent_id", agent.getAgent_id());
    sim_data.put("wealth", agent.getWealth());
    return sim_data;
  }

  public void sendAgentsData(Model model) {
    // send Agent agent's data
    for (Agent temp_agt: model.agentsToDB()) {
      kafkaTemplate.send("db", "agents_data", getAgentData(model, temp_agt));
    }
  }

  public void sendChartingData(Model model) {
    kafkaTemplate.send("chart", "Top10 Wealth", model.lineChart());
  }

  public void sendVisualData(Model model) {
    int agents_popln = model.toVisual().size();
    for (Agent temp_agt: model.toVisual()) {
      kafkaTemplate.send("visuals", String.valueOf(agents_popln), temp_agt.agentVisual());
    }
  }
}
