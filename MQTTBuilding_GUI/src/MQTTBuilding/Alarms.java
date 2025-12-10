package MQTTBuilding;

import org.eclipse.paho.client.mqttv3.*;

public class Alarms {
	
	public static MqttClient client;
	public static boolean active = false;
	
	public static void getAlarmStatus() throws MqttPersistenceException, MqttException {
		String msg = "Alarme" + (active ? "Ativado" : "Desativado");
		client.publish(MQTTConfig.ALARM_TOPIC, new MqttMessage(msg.getBytes()));
	}
	public static void setAlarmStatus() throws MqttPersistenceException, MqttException {
		active = !active;
		String msg = "Estado do alarme alterado para " + (active ? "Ativado" : "Desativado");
		client.publish(MQTTConfig.ALARM_TOPIC, new MqttMessage(msg.getBytes()));
	}
	
	public static void activateAlarms() throws MqttPersistenceException, MqttException {
		client.connect();
		client.subscribe(MQTTConfig.COMMAND_TOPIC);
		String msg = "Alarme conectado";
		client.publish(MQTTConfig.ALARM_TOPIC, new MqttMessage(msg.getBytes()));
	}
	
	public static void deactivateAlarms() throws MqttPersistenceException, MqttException {
		String msg = "Alarme desconectado";
		client.publish(MQTTConfig.ALARM_TOPIC, new MqttMessage(msg.getBytes()));
		
		client.disconnect();
		
		System.exit(0);
	}
	
	private static void processarComando(String comando) throws Exception {

        switch (comando) {

            case "getAlarmStatus":
                getAlarmStatus();
                break;

            case "setAlarmStatus":
                setAlarmStatus();
                break;
                
            case "deactivateAlarms":
            	deactivateAlarms();
            	break;

            default:
                break;
        }
    }

	public static void main(String[] args) throws MqttException {
		client = new MqttClient(MQTTConfig.BROKER, "alarm01");
		
		activateAlarms();
		
		client.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage msg) throws Exception {

                String payload = new String(msg.getPayload());

                processarComando(payload);
            }

            @Override public void connectionLost(Throwable cause) {}
            @Override public void deliveryComplete(IMqttDeliveryToken token) {}
        });
	}

}
