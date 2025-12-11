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

        Color bg = new Color(40, 40, 40);
        Color panelBG = new Color(55, 55, 55);
        Color buttonBG = new Color(70, 70, 70);
        Color buttonBorder = new Color(110, 110, 110);
        Color textColor = new Color(230, 230, 230);

        UIManager.put("OptionPane.background", bg);
        UIManager.put("Panel.background", bg);
        UIManager.put("OptionPane.messageForeground", textColor);

        setLayout(new BorderLayout());
        setSize(700, 550);
        getContentPane().setBackground(bg);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel titulo = new JLabel("Central de Monitoramento", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 26));
        titulo.setForeground(textColor);
        titulo.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titulo, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(new Color(25, 25, 25));
        logArea.setForeground(new Color(0, 220, 0));
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(buttonBorder, 1));
        add(scroll, BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel linha1 = new JPanel(new GridLayout(1, 4, 10, 10));
        JPanel linha2 = new JPanel(new GridLayout(1, 4, 10, 10));
        linha1.setBackground(bg);
        linha2.setBackground(bg);

        JButton btnStatusSensor = criarBotao("Status de 1 Sensor", buttonBG, textColor, buttonBorder);
        JButton btnStatusAll    = criarBotao("Todos Sensores", buttonBG, textColor, buttonBorder);
        JButton btnAbrir        = criarBotao("Abrir Entrada", buttonBG, textColor, buttonBorder);
        JButton btnFechar       = criarBotao("Fechar Entrada", buttonBG, textColor, buttonBorder);

        JButton btnFecharTudo   = criarBotao("Fechar Todas", buttonBG, textColor, buttonBorder);
        JButton btnAlarmStatus  = criarBotao("Status Alarme", buttonBG, textColor, buttonBorder);
        JButton btnToggleAlarm  = criarBotao("Alternar Alarme", buttonBG, textColor, buttonBorder);
        JButton btnSair         = criarBotao("Sair", new Color(120, 30, 30), Color.WHITE, buttonBorder);

        linha1.add(btnStatusSensor);
        linha1.add(btnStatusAll);
        linha1.add(btnAbrir);
        linha1.add(btnFechar);

        linha2.add(btnFecharTudo);
        linha2.add(btnAlarmStatus);
        linha2.add(btnToggleAlarm);
        linha2.add(btnSair);

        panel.add(linha1);
        panel.add(linha2);
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
            } catch (Exception ex) {
            	ex.printStackTrace();
            }
                
            try {
                System.exit(0);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        setVisible(true);
    }

    private JButton criarBotao(String texto, Color bg, Color fg, Color borda) {
        JButton btn = new JButton(texto);
        btn.setFocusPainted(false);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createLineBorder(borda, 2));
        btn.setPreferredSize(new Dimension(140, 45));
        return btn;
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
                    logArea.append("Conexão perdida: " + cause.getMessage() + "\n");
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.subscribe(MQTTConfig.ALARM_TOPIC + "/#");
            client.subscribe(MQTTConfig.SENSORS_TOPIC + "/#");

            logArea.append("Conectado ao broker: " + MQTTConfig.BROKER + "\n");

        } catch (Exception e) {
            logArea.append("Erro ao conectar MQTT: " + e.getMessage() + "\n");
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
            this, "Selecione o sensor:",
            "Sensores", JOptionPane.QUESTION_MESSAGE,
            null, sensores, sensores[0]
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
            this, "Selecione o sensor:",
            "Sensores", JOptionPane.QUESTION_MESSAGE,
            null, sensores, sensores[0]
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
