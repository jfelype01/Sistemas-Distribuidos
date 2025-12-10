package MQTTBuilding;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.paho.client.mqttv3.*;

public class CentralGUI extends JFrame {

    private JTextArea logArea;
    private MqttClient client;
    private static final String LOG_FILE = "./log/central_log.txt";

    public CentralGUI() {
    	
        super("Central de Segurança – Interface Gráfica");

        setLayout(new BorderLayout());
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 10, 10));

        JButton btnStatusSensor = new JButton("Ver status de 1 sensor");
        JButton btnStatusAll = new JButton("Ver todos sensores");
        JButton btnAbrir = new JButton("Abrir entrada");
        JButton btnFechar = new JButton("Fechar entrada");
        JButton btnFecharTudo = new JButton("Fechar todas entradas");
        JButton btnAlarmStatus = new JButton("Status do alarme");
        JButton btnToggleAlarm = new JButton("Alternar alarme");
        JButton btnSair = new JButton("Sair");

        panel.add(btnStatusSensor);
        panel.add(btnStatusAll);
        panel.add(btnAbrir);
        panel.add(btnFechar);
        panel.add(btnFecharTudo);
        panel.add(btnAlarmStatus);
        panel.add(btnToggleAlarm);
        panel.add(btnSair);

        add(panel, BorderLayout.SOUTH);

        conectarMQTT();

        btnStatusSensor.addActionListener(e -> escolherSensor("getStatus"));
        btnStatusAll.addActionListener(e -> enviarComando("getStatusAll", null));
        btnAbrir.addActionListener(e -> escolherSensorAbrirFechar(true));
        btnFechar.addActionListener(e -> escolherSensorAbrirFechar(false));
        btnFecharTudo.addActionListener(e -> enviarComando("closeAll", null));
        btnAlarmStatus.addActionListener(e -> enviarComando("getAlarmStatus", null));
        btnToggleAlarm.addActionListener(e -> enviarComando("setAlarmStatus", null));

        btnSair.addActionListener(e -> {
            try {
                enviarComando("deactivateAlarms", null);
                enviarComando("deactivateSensors", null);

                salvarLog("SYSTEM", "Central desligada.");
                
                client.disconnect();
                System.exit(0);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        setVisible(true);
    }

    private void conectarMQTT() {
        try {
            client = new MqttClient(MQTTConfig.BROKER, "centralGUI");

            MqttConnectOptions options = new MqttConnectOptions();
            client.connect(options);

            client.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    logArea.append("[" + topic + "] " + payload + "\n");
                    
                    salvarLog(topic, payload);
                }

                @Override public void connectionLost(Throwable cause) {
                	System.out.println("Conexão perdida: " + cause.getMessage());
                    salvarLog("SYSTEM", "Conexão perdida: " + cause.getMessage());
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.subscribe(MQTTConfig.ALARM_TOPIC + "/#");
            client.subscribe(MQTTConfig.SENSORS_TOPIC + "/#");

            logArea.append("Conectado ao broker: " + MQTTConfig.BROKER + "\n");

        } catch (Exception e) {
            logArea.append("Erro ao conectar MQTT: " + e.getMessage() + "\n");

            salvarLog("SYSTEM", "Erro fatal: " + e.getMessage());
        }
    }

    private void enviarComando(String comando, String dados) {

        try {
            String payload = (dados != null) ? comando + "|" + dados : comando;

            client.publish(MQTTConfig.COMMAND_TOPIC, new MqttMessage(payload.getBytes()));

            logArea.append("[COMANDO] " + payload + "\n");

            salvarLog("COMMAND", payload);

        } catch (Exception e) {
            logArea.append("Erro ao enviar comando: " + e.getMessage() + "\n");
        }
    }

    private void escolherSensor(String operacao) {
        String[] sensores = {
            "porta_principal",
            "porta_secundaria",
            "porta_fundos"
        };

        String sensor = (String) JOptionPane.showInputDialog(
                this,
                "Selecione o sensor:",
                "Sensores",
                JOptionPane.QUESTION_MESSAGE,
                null,
                sensores,
                sensores[0]
        );

        if (sensor != null)
            enviarComando(operacao, sensor);
    }

    private void escolherSensorAbrirFechar(boolean abrir) {
        String[] sensores = {
            "porta_principal",
            "porta_secundaria",
            "porta_fundos"
        };

        String sensor = (String) JOptionPane.showInputDialog(
                this,
                "Selecione o sensor:",
                "Sensores",
                JOptionPane.QUESTION_MESSAGE,
                null,
                sensores,
                sensores[0]
        );

        if (sensor != null)
            enviarComando("setStatus", sensor + ";" + abrir);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CentralGUI::new);
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
}
