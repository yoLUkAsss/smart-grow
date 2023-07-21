package com.example.gateway.tasks;

import com.example.gateway.data.Environment;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.SneakyThrows;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

@Component
public class BrokerConsumer {

    private static final Logger Log = LoggerFactory.getLogger("broker.consumer." + BrokerConsumer.class);

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private InfluxDBClient influxDBClient;

    private static final Logger log = LoggerFactory.getLogger(BrokerConsumer.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public void start(){
        log.info("The time is now {}", dateFormat.format(new Date()));


        mqttClient.setCallback(new MqttCallback() {
            @SneakyThrows
            @Override
            public void connectionLost(Throwable cause) {
                Log.error("Connection with Broker lost [cause: {}]", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                log.info("Message arrived: [data: {}]", message);

                ByteArrayInputStream in = new ByteArrayInputStream(message.getPayload());
                ObjectInputStream is = new ObjectInputStream(in);
                Integer parsedData = (Integer) is.readObject();
                in.close();



                Point point = Point
                        .measurement(measureBasedOnTopic(topic))
                        .addTag("type", "integer")
                        .addField("value", parsedData)
                        .time(Instant.now(), WritePrecision.MS);

                WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
                writeApi.writePoint("my-init-bucket", "my-init-org", point);


                Log.info("Message arrived - [topic: {}] [message: {}]", topic, parsedData);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.info("Message with token: {} enviado correctamente", token);
            }
        });

    }

    private String measureBasedOnTopic(String topic) {
        if (topic.equals("/grow-environment/temperature")) {
            return "temperature";
        } else if (topic.equals("/grow-environment/humidity")) {
            return "humidity";
        } else if (topic.equals("/grow-environment/co2")) {
            return "co2";
        }
        return null;
    }

}
