package com.acme.rabbitmq.subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

@Component
public class CommandListener {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${queue.request}", durable = "${queue.durable}"),
            exchange = @Exchange(value = "${exchange.name}", type = "${exchange.type}", durable = "${exchange.durable}"),
            key = "${routingKey.name}"))
    public Message consume(Message message) throws IOException, InterruptedException {

        String body = new String(message.getBody());
        log.info("Received command: " + body);

        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", body);
        Process p = pb.start();

        int exitCode = p.waitFor();
        log.info("Exit Code: " + exitCode);

        InputStreamReader isReader = new InputStreamReader(p.getInputStream());
        //Creating a BufferedReader object
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str + "\n");
        }

        String commandOutput = sb.toString();
        log.info("Command Output: " + commandOutput);

        MessageProperties replyMessageProperties = message.getMessageProperties();
        Message replyMessage = new Message(commandOutput.getBytes(), replyMessageProperties);
        log.info("Completed processing and sending the message :" + message);

        return replyMessage;
    }
}
