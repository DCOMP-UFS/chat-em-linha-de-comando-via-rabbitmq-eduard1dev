package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Chat {

    private static final String EXCHANGE_NAME = "chat-dcomp";
    private static String username;
    private static String currentRecipient;

    public static void main(String[] args) {
        try {
            // Criação de uma conexão e um canal RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("44.202.157.190"); // Alterar
            factory.setUsername("admin"); // Alterar
            factory.setPassword("{PASSWORD}"); // Alterar
            factory.setVirtualHost("/");
            try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
                // Declaração da exchange
                channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);

                // Pergunta pelo nome do usuário
                System.out.print("User: ");
                username = new BufferedReader(new InputStreamReader(System.in)).readLine();

                // Cria a fila do usuário
                String queueName = channel.queueDeclare(username, false, false, false, null).getQueue();

                // Faz o binding da fila à exchange
                channel.queueBind(queueName, EXCHANGE_NAME, username);

                // Inicia thread para receber mensagens
                new Thread(() -> receiveMessages(channel, queueName)).start();

                // Inicia o prompt para envio de mensagens
                System.out.print(">> ");
                startMessagePrompt(channel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void receiveMessages(Channel channel, String queueName) {
        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String sender = delivery.getEnvelope().getRoutingKey();
                System.out.println("\n(" + delivery.getProperties().getTimestamp() + ") " + sender + " diz: " + message);
                if (currentRecipient != null) {
                    System.out.print("@" + currentRecipient + ">> ");
                } else {
                    System.out.print(">> ");
                }
            };

            // Registra o callback para receber mensagens
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startMessagePrompt(Channel channel) {
        try {
            while (true) {
                if (currentRecipient != null) {
                    System.out.print("@" + currentRecipient + ">> ");
                }
                
                String input = new BufferedReader(new InputStreamReader(System.in)).readLine();

                if (input.startsWith("@")) {
                    // Muda o destinatário
                    currentRecipient = input.substring(1);
                } else {
                    // Envia a mensagem
                    String message = input;
                    channel.basicPublish(EXCHANGE_NAME, currentRecipient, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
