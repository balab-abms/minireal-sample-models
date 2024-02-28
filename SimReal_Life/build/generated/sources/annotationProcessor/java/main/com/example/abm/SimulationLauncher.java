package com.example.abm;

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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

public class SimulationLauncher implements Runnable {
  private KafkaTemplate<String, Object> kafkaTemplate;

  private HashMap<String, Object> sim_params;

  private String user_id;

  public SimulationLauncher(KafkaTemplate<String, Object> kafkaTemplate,
      HashMap<String, Object> sim_params, String user_id) {
    this.kafkaTemplate = kafkaTemplate;
    this.sim_params = sim_params;
    this.user_id = user_id;
  }

  @Override
  public void run() {
    // setup simulation and model arguments
    int population = Integer.parseInt(sim_params.containsKey("population") ? sim_params.get("population").toString() : "50");
    int world_sz = Integer.parseInt(sim_params.containsKey("world_sz") ? sim_params.get("world_sz").toString() : "15");
    int steps = Integer.parseInt(sim_params.containsKey("steps") ? sim_params.get("steps").toString() : "100");
    int tick_delay = Integer.parseInt(sim_params.containsKey("tick_delay") ? sim_params.get("tick_delay").toString() : "100");
    Life model = new Life(population, world_sz);
    do {
      boolean is_step = model.schedule.step(model);
      if (!is_step) break;
      sendKafkaData("tick", "ui_token", model.schedule.getSteps());
      // send database data;
      sendAgentsData(model);
      // send charting data;
      sendChartingData(model);
      // send visualization data;
      sendVisualData(model);
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
      // get user id;
      String user_id = args.length > 2 ? args[2] : "0";
      // run simulation;
      Runnable model_obj = new SimulationLauncher(kafkaTemplate_deserialized, modelParams_deserialized, user_id);
      Thread model_thread = new Thread(model_obj);
      model_thread.start();
    } catch(IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void sendKafkaData(String topic, String key, Object value) {
    // Create a new producer record for the data and headers
    ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, value);
    // Add headers
    record.headers().add(new RecordHeader("user_id", user_id.getBytes()));
    // Send the record to kafka broker
    kafkaTemplate.send(record);
  }

  public Map<String, Object> getAgentData(Life model, Agent agent) {
    Map<String, Object> sim_data = new LinkedHashMap<>();
    sim_data.put("agent_id", agent.getAgent_id());
    sim_data.put("alive", agent.getAlive());
    return sim_data;
  }

  public void sendAgentsData(Life model) {
    // send Agent agent's data
    for (Agent temp_agt: model.persistAgentsData()) {
      sendKafkaData("db", "db", getAgentData(model, temp_agt));
    }
  }

  public void sendChartingData(Life model) {
    sendKafkaData("chart", "alive_cells", model.drawChart());
  }

  public void sendVisualData(Life model) {
    int agents_popln = model.sendVisualsData().size();
    for (Agent temp_agt: model.sendVisualsData()) {
      sendKafkaData("visuals", String.valueOf(agents_popln), temp_agt.agentVisual());
    }
  }
}
