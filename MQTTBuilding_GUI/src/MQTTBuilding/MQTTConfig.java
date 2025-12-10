package MQTTBuilding;

public class MQTTConfig {
	public static final String BROKER = "tcp://localhost:1099";
	
	public static final String ALARM_TOPIC = "predio/logs/alarms";
	public static final String SENSORS_TOPIC = "predio/logs/sensors";
	public static final String COMMAND_TOPIC = "predio/logs/comands";
	
	public static final String CONSUMER = "CentraSeguranca";
}
