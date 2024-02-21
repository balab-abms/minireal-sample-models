package com.example.application;

import com.example.application.kafkaserialize.KafkaTemplateSerializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimulationLauncherManual implements Runnable {
    private String table_name = "sample_table";
    private KafkaTemplate<String, Object> kafkaTemplate;
    private HashMap<String, Object> simParameters;
    public SimulationLauncherManual(KafkaTemplate<String, Object> kafkaTemplate, HashMap<String, Object> simParameters) {
        this.kafkaTemplate = kafkaTemplate;
        this.simParameters = simParameters;
    }
    @Override
    public void run() {
        // setup simulation and model arguments
        int steps = simParameters.containsKey("steps") ? (int)Double.parseDouble(simParameters.get("steps").toString()) : 100;
        int tick_delay = simParameters.containsKey("tick_delay") ? (int)Double.parseDouble(simParameters.get("tick_delay").toString())  : 100;
        String ui_token = simParameters.containsKey("ui_token") ? simParameters.get("ui_token").toString() : null;
        int population = simParameters.containsKey("population") ? (int)Double.parseDouble(simParameters.get("population").toString()) : 50;
        int wealth = simParameters.containsKey("wealth") ? (int)Double.parseDouble(simParameters.get("wealth").toString()) : 50;
        Model model = new Model(population, wealth);
        do {
            kafkaTemplate.send("tick", model.schedule.getSteps());      // check if the kafka template exists ... send tick count
            kafkaTemplate.send("chart", "top10", model.lineChart());    // send top10% wealth
            model.getAgents_list().forEach(agt -> { // send visual and db data
                kafkaTemplate.send("db", table_name, agentData(model, agt));
                kafkaTemplate.send("visuals", String.valueOf(model.getPopulation()), agt.agentVisual());
            });
            boolean is_step = model.schedule.step(model); // step the model
            if (!is_step) break;
            try {   // have some gap between simulation ticks so that ABMS microservice system process ... data properly
                Thread.sleep(tick_delay);
            } catch (InterruptedException e) { throw new RuntimeException(e); }
        } while (model.schedule.getSteps() < steps);
    }
    public Map<String, Object> agentData(Model model, Agent agent) {
        Map<String, Object> sim_data = new LinkedHashMap<>();
        sim_data.put("step", model.schedule.getSteps());
        sim_data.put("wealth", agent.getWealth());
        return sim_data;
    }
    public static void main(String[] args) {
        try {
            // get kafka template from args
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
            KafkaTemplateSerializer kafkaTemplateSerializer = (KafkaTemplateSerializer) ois.readObject();
            ois.close();
            KafkaTemplate<String, Object> kafkaTemplate_deserialized = kafkaTemplateSerializer.getKafkaTemplate();
            // get model parameters from args
            Type hash_type = new TypeToken<HashMap<String, Object>>(){}.getType();
            HashMap<String, Object> modelParams_deserialized = args.length > 1 ? new Gson().fromJson(args[1], hash_type) : new HashMap<>();
            System.out.println(modelParams_deserialized.toString());
            // run simulation
            Runnable model_obj = new SimulationLauncherManual(kafkaTemplate_deserialized, modelParams_deserialized);
            Thread model_thread = new Thread(model_obj);
            model_thread.start();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
}}}
