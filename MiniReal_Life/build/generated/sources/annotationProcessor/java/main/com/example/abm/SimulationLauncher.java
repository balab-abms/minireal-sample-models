package com.example.abm;

import com.example.application.kafkaserialize.KafkaTemplateSerializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ClassNotFoundException;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.Runnable;
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

  private String sim_session_token;

  private String batch_comb_name;

  public SimulationLauncher(KafkaTemplate<String, Object> kafkaTemplate,
      HashMap<String, Object> sim_params, String sim_session_token, String batch_comb_name) {
    this.kafkaTemplate = kafkaTemplate;
    this.sim_params = sim_params;
    this.sim_session_token = sim_session_token;
    this.batch_comb_name = batch_comb_name;
  }

  @Override
  public void run() {
    // setup simulation and model arguments
    int population = Integer.parseInt(sim_params.containsKey("population") ? sim_params.get("population").toString() : "50");
    int world_sz = Integer.parseInt(sim_params.containsKey("world_sz") ? sim_params.get("world_sz").toString() : "15");
    int steps = Integer.parseInt(sim_params.containsKey("steps") ? sim_params.get("steps").toString() : "100");
    try {
      Life model = new Life(population, world_sz);
      do {
        boolean is_step = model.schedule.step(model);
        if (!is_step) break;
        kafkaTemplate.send("tick" + sim_session_token, model.schedule.getSteps());
        // send database data;
        sendAgentsData(model);
        // send charting data;
        sendChartingData(model);
        Thread.sleep(250);
      }
      while (model.schedule.getSteps() < steps);
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
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
      // get user simulation session id;
      String sim_session_token = args.length > 2 ? args[2] : "0";
      // get batch combination name;
      String batch_comb_name = args.length > 3 ? args[3] + "_" : "";
      // run simulation;
      Runnable model_obj = new SimulationLauncher(kafkaTemplate_deserialized, modelParams_deserialized, sim_session_token, batch_comb_name);
      Thread model_thread = new Thread(model_obj);
      model_thread.start();
    } catch(IOException | ClassNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void sendKafkaData(Life model, String topic, String key, Object value) {
    // Create a new producer record for the data and headers
    ProducerRecord<String, Object> record = new ProducerRecord<>(topic + sim_session_token, key, value);
    // Add headers
    record.headers().add(new RecordHeader("tick", String.valueOf(model.schedule.getSteps()).getBytes()));
    record.headers().add(new RecordHeader("sim_session_token", sim_session_token.getBytes()));
    record.headers().add(new RecordHeader("signal", "data".getBytes()));
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
      sendKafkaData(model, "db", "db", getAgentData(model, temp_agt));
    }
  }

  public void sendChartingData(Life model) {
    sendKafkaData(model, "chart", batch_comb_name + "alive_cells", model.drawChart());
  }
}
