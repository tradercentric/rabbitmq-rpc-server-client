package com.acme.rabbitmq;

import com.acme.rabbitmq.config.Crypto;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.*;

public class CommandSender implements AutoCloseable {

    private Connection connection;
    private Channel channel;

    private String applicationKey = "volcano";
    private String host = "localhost";
    private Integer port = 5672;
    private String username = "guest";
    private String scrambled = "amU7yEqKxAkI7/n+pAwJOQ==";
    private String requestQueueName = "q.acme.command";
    private String replyQueueName = "amq.rabbitmq.reply-to";
    private String exchangeName = "x.acme";
    private String routingKey = "rpc";
    private boolean durable = false;

    public CommandSender() throws IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException, URISyntaxException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(Crypto.decrypt(scrambled, applicationKey));
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(requestQueueName, durable, false, false, null);
        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, durable);
        channel.queueBind(requestQueueName, exchangeName, routingKey);
    }

    public static void main(String[] argv) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, InterruptedException, ExecutionException {

        long timeout = 10L;
        String command = "dir";

        try {
            FutureTask<String> timeoutTask = new FutureTask<String>(
                    new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            try (CommandSender commandSender = new CommandSender()) {
                                return commandSender.execute("dir");
                            }
                        }
                    }
            );

            new Thread(timeoutTask).start();
            String response = timeoutTask.get(timeout, TimeUnit.SECONDS);
            System.out.println(response);
        } catch (TimeoutException e) {
            System.err.printf("Command \"%s\" got timeout after reaching %s seconds", command, timeout);
            System.exit(1);
        }
    }

    public String execute(String message) throws IOException, InterruptedException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}
