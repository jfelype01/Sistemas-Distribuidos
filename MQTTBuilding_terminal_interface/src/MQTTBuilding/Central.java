package MQTTBuilding;

import org.eclipse.paho.client.mqttv3.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Central {

    private static MqttClient client;

    private static final String LOG_FILE = "./log/central_log.txt";

    public static void main(String[] args) {

        try {
            client = new MqttClient(MQTTConfig.BROKER, "central");
            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            if (client.isConnected()) {

                System.out.println("Conectado ao broker MQTT!");

                client.setCallback(new MqttCallback() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String payload = new String(message.getPayload());

                        String output = "[MQTT RECEBIDO] (" + topic + "): " + payload;
                        System.out.println("\n" + output);
                        System.out.print("\n> ");

                        salvarLog(topic, payload);
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        System.out.println("Conexão perdida: " + cause.getMessage());
                        salvarLog("SYSTEM", "Conexão perdida: " + cause.getMessage());
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                client.subscribe(MQTTConfig.ALARM_TOPIC + "/#");
                client.subscribe(MQTTConfig.SENSORS_TOPIC + "/#");

                abrirTerminal();
            }

        } catch (Exception e) {
            salvarLog("SYSTEM", "Erro fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void salvarLog(String topic, String mensagem) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {

            String tempo = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            writer.write(tempo + " | " + topic + " | " + mensagem + "\n");

        } catch (IOException e) {
            System.out.println("Erro ao escrever no log: " + e.getMessage());
        }
    }

    private static void abrirTerminal() throws Exception {
        Scanner sc = new Scanner(System.in);

        while (true) {

            System.out.println("\n===== MENU DA CENTRAL =====");
            System.out.println("1 - Ver status de um sensor");
            System.out.println("2 - Ver status de todos sensores");
            System.out.println("3 - Abrir entrada");
            System.out.println("4 - Fechar entrada");
            System.out.println("5 - Fechar todas portas");
            System.out.println("6 - Ver status do alarme");
            System.out.println("7 - Alternar estado do alarme");
            System.out.println("0 - Sair");
            System.out.print("> ");

            int opcao = Integer.parseInt(sc.nextLine());
            int opcao_sensor = 0;

            switch (opcao) {
                case 1:
                    System.out.print("Nome do sensor: ");
                    System.out.println("1 - Porta Principal");
                    System.out.println("2 - Porta Secundária");
                    System.out.println("3 - Porta dos Fundos");
                    
                    do {
	                    opcao_sensor = Integer.parseInt(sc.nextLine());
	                    
	                    switch (opcao_sensor) {
	                    	case 1:
	                    		enviarComando("getStatus", "porta_principal");
	                    		break;
	                		case 2:
	                    		enviarComando("getStatus", "porta_secundaria");
	                    		break;
	                    	case 3:
	                    		enviarComando("getStatus", "porta_fundos");
	                    		break;
	                		default:
	                			System.out.println("Sensor inválido");
	                			break;
	                    }
	                    if (opcao_sensor >= 1 && opcao_sensor <= 3) break;
                    } while (opcao_sensor < 1 || opcao_sensor > 3);
                    
                    break;
                    
                case 2:
                    enviarComando("getStatusAll", null);
                    break;

                case 3:
                    System.out.print("Sensor a abrir: ");
                    System.out.println("1 - Porta Principal");
                    System.out.println("2 - Porta Secundária");
                    System.out.println("3 - Porta dos Fundos");
                    
                    do {
	                    opcao_sensor = Integer.parseInt(sc.nextLine());
	                    
	                    switch (opcao_sensor) {
	                    	case 1:
	                    		enviarComando("setStatus", "porta_principal" + ";true");
	                    		break;
	                		case 2:
	                    		enviarComando("setStatus", "porta_secundaria" + ";true");
	                    		break;
	                    	case 3:
	                    		enviarComando("setStatus", "porta_fundos" + ";true");
	                    		break;
	                		default:
	                			System.out.println("Sensor inválido");
	                			break;
	                    }
	                    if (opcao_sensor >= 1 && opcao_sensor <= 3) break;
                    } while (opcao_sensor < 1 || opcao_sensor > 3);
                    
                    break;

                case 4:
                    System.out.print("Sensor a fechar: ");
                    System.out.println("1 - Porta Principal");
                    System.out.println("2 - Porta Secundária");
                    System.out.println("3 - Porta dos Fundos");
                    
                    do {
	                    opcao_sensor = Integer.parseInt(sc.nextLine());
	                    
	                    switch (opcao_sensor) {
	                    	case 1:
	                    		enviarComando("setStatus", "porta_principal" + ";false");
	                    		break;
	                		case 2:
	                    		enviarComando("setStatus", "porta_secundaria" + ";false");
	                    		break;
	                    	case 3:
	                    		enviarComando("setStatus", "porta_fundos" + ";false");
	                    		break;
	                		default:
	                			System.out.println("Sensor inválido");
	                			break;
	                    }
	                    if (opcao_sensor >= 1 && opcao_sensor <= 3) break;
                    } while (opcao_sensor < 1 || opcao_sensor > 3);
                    break;

                case 5:
                    enviarComando("closeAll", null);
                    break;

                case 6:
                    enviarComando("getAlarmStatus", null);
                    break;

                case 7:
                    enviarComando("setAlarmStatus", null);
                    break;

                case 0:
                	salvarLog("SYSTEM", "Central desligada.");
                    enviarComando("deactivateAlarms", null);
                    enviarComando("deactivateSensors", null);
                    
                    try {
                    	Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    	System.out.println(e);
                    }
                    

                    System.out.println("Encerrando...");
                    client.disconnect();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Opção inválida!");
            }
            
            try {
            	Thread.sleep(500);
            } catch (InterruptedException e) {
            	System.out.println(e);
            }
        }
    }

    private static void enviarComando(String comando, String dados) throws Exception {

        String payload;

        if (dados != null)
            payload = comando + "|" + dados;
        else
            payload = comando;

        client.publish(
                MQTTConfig.COMMAND_TOPIC,
                new MqttMessage(payload.getBytes())
        );

        System.out.println("COMANDO ENVIADO " + payload);

        salvarLog("COMMAND", payload);
    }
}
