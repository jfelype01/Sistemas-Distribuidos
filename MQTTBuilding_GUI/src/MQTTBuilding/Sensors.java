package MQTTBuilding;

import org.eclipse.paho.client.mqttv3.*;

import java.util.HashMap;
import java.util.Map;

public class Sensors {

    private static MqttClient client;

    private static Map<String, Boolean> sensores = new HashMap<>();
    
    public static void createSensors() throws Exception {
    	
    	sensores.put("porta_principal", false);
        sensores.put("porta_secundaria", false);
        sensores.put("porta_fundos", false);
    }

    public static void activateSensors() throws Exception {
        
        client.connect();
        client.subscribe(MQTTConfig.COMMAND_TOPIC);
        String msg = "Sensores conectados";
        client.publish(MQTTConfig.SENSORS_TOPIC, new MqttMessage(msg.getBytes()));
    }
    
    public static void deactivateSensors() throws Exception {
        
        String msg = "Sensores desconectados";
        client.publish(MQTTConfig.SENSORS_TOPIC, new MqttMessage(msg.getBytes()));
        

        client.disconnect();
        
        System.exit(0);
    }

    public static void getStatus(String sensor) throws MqttPersistenceException, MqttException {
    	String msg;
        Boolean status = sensores.get(sensor);
        if (status == null) msg =  "Sensor" + sensor + " não encontrado.";

        msg = sensor + ": " + (status ? "ABERTA" : "FECHADA");
        client.publish(MQTTConfig.SENSORS_TOPIC, new MqttMessage(msg.getBytes()));
    }

    public static void getStatusAll() throws MqttPersistenceException, MqttException {
        StringBuilder sb = new StringBuilder();
        sensores.forEach((nome, estado) -> {
            sb.append(nome)
              .append(": ")
              .append(estado ? "ABERTA" : "FECHADA")
              .append("\n");
        });
        String msg = sb.toString();
        client.publish(MQTTConfig.SENSORS_TOPIC, new MqttMessage(msg.getBytes()));
    }

    public static void setStatus(String sensor, boolean aberto) throws Exception {
        if (!sensores.containsKey(sensor)) {
            System.out.println("Sensor não encontrado: " + sensor);
            return;
        }

        sensores.put(sensor, aberto);

        String msg = "Status alterado → " + sensor + " agora está " + (aberto ? "ABERTA" : "FECHADA");

        client.publish(MQTTConfig.SENSORS_TOPIC, new MqttMessage(msg.getBytes()));
    }

    public static void closeAll() throws Exception {
        sensores.keySet().forEach(key -> {
            try {
                sensores.put(key, false);

                String msg = "FecharTodas → " + key + " foi FECHADA.";
                client.publish(MQTTConfig.SENSORS_TOPIC, new MqttMessage(msg.getBytes()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private static void processarComando(String comando) throws Exception {

        String[] partes = comando.split("\\|");

        String operacao = partes[0];
        String dados = (partes.length > 1) ? partes[1] : null;

        switch (operacao) {

            case "getStatus":
                getStatus(dados);
                break;

            case "getStatusAll":
                getStatusAll();
                break;

            case "setStatus":
                String[] args = dados.split(";");
                setStatus(args[0], Boolean.parseBoolean(args[1]));
                break;

            case "closeAll":
                closeAll();
                break;
                
            case "deactivateSensors":
            	deactivateSensors();
            	break;

            default:
            	break;
        }
    }

    public static void main(String[] args) throws Exception {
        client = new MqttClient(
            MQTTConfig.BROKER,
            "sensorEntradasController"
        );
        
        client.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                String payload = new String(message.getPayload());

                processarComando(payload);
            }

            @Override public void connectionLost(Throwable cause) {}
            @Override public void deliveryComplete(IMqttDeliveryToken token) {}
        });
        
        createSensors();
        activateSensors();
    }
}
