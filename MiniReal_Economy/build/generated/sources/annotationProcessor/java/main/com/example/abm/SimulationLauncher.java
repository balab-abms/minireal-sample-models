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
  private String sim_session_token;
  public SimulationLauncher(KafkaTemplate<String, Object> kafkaTemplate, HashMap<String, Object> sim_params, String sim_session_token) {
    this.kafkaTemplate = kafkaTemplate;
    this.sim_params = sim_params;
    this.sim_session_token = sim_session_token;
  }
  @Override
  public void run() {
    int population = Integer.parseInt(sim_params.containsKey("population") ? sim_params.get("population").toString() : "50");       // setup simulation and model arguments
    int wealth = Integer.parseInt(sim_params.containsKey("wealth") ? sim_params.get("wealth").toString() : "50");
    int steps = Integer.parseInt(sim_params.containsKey("steps") ? sim_params.get("steps").toString() : "100");
    Model model = new Model(population, wealth);
    do {
      boolean is_step = model.schedule.step(model);
      if (!is_step) break;
      sendKafkaData("tick", "", model.schedule.getSteps());
      sendAgentsData(model);        // send database data;
      sendChartingData(model);      // send charting data;
      try {  Thread.sleep(250);}
      catch(InterruptedException e) { throw new RuntimeException(e); }
    } while (model.schedule.getSteps() < steps);
  }
  public static void main(String[] args) {
    try {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));      // get kafka template from args;
      KafkaTemplateSerializer kafkaTemplateSerializer = (KafkaTemplateSerializer) ois.readObject();
      ois.close();
      KafkaTemplate<String, Object> kafkaTemplate_deserialized = kafkaTemplateSerializer.getKafkaTemplate();
      Type hash_type = new TypeToken<HashMap<String, Object>>(){}.getType();    // get model parameters from args;
      HashMap<String, Object> modelParams_deserialized = args.length > 1 ? new Gson().fromJson(args[1], hash_type) : new HashMap<>();
      String sim_session_token = args.length > 2 ? args[2] : "0"; // get user id;
      Runnable model_obj = new SimulationLauncher(kafkaTemplate_deserialized, modelParams_deserialized, sim_session_token);    // run simulation;
      Thread model_thread = new Thread(model_obj);
      model_thread.start();
    } catch(IOException | ClassNotFoundException e) { throw new RuntimeException(e);  }
  }
  public void sendKafkaData(String topic, String key, Object value) {
    // Create a new producer record for the data and headers
    ProducerRecord<String, Object> record = new ProducerRecord<>(topic + sim_session_token, key, value);
    record.headers().add(new RecordHeader("sim_session_token", sim_session_token.getBytes()));      // Add headers
    record.headers().add(new RecordHeader("signal", "data".getBytes()));
    kafkaTemplate.send(record);           // Send the record to kafka broker
  }
  public Map<String, Object> getAgentData(Model model, Agent agent) {
    Map<String, Object> sim_data = new LinkedHashMap<>();
    sim_data.put("agent_id", agent.getAgent_id());
    sim_data.put("wealth", agent.getWealth());
    sim_data.put("step", agent.getStep());
    return sim_data;
  }
  public void sendAgentsData(Model model) {
    for (Agent temp_agt: model.agentsToDB()) {   sendKafkaData("db", "agents_data", getAgentData(model, temp_agt));     }
  }
  public void sendChartingData(Model model) {
    sendKafkaData("chart", "Top10 Wealth", model.lineChart());
  }
}
