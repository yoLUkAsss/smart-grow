package com.example.gateway;

import com.example.gateway.data.Environment;
import com.example.gateway.tasks.TemperatureCallback;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.SneakyThrows;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.time.Instant;

@EnableScheduling
@SpringBootApplication
public class GatewayApplication {

	private static final Logger Log = LoggerFactory.getLogger("main.app." + GatewayApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public TemperatureCallback temperatureCallback() {
		return new TemperatureCallback();
	}

	@Bean
	public InfluxDBClient influxDBClient() {

		Log.info("Connecting with Influx DB...");

		String token = "BEv7n0hsVt8OMJnwTXnBPFgfJE71EOovWTvfd_uu1umUxMgIofr4H6zG8UNoksDMwEok9Kesxu-FfN46Frf_eA==";
		String org = "my-init-org";
		String bucket = "my-init-bucket";
		String host = "http://localhost:8086";

		try {
			Log.info("Attempting connection with [host: {}] - [org: {}] - [bucket: {}]", host, org, bucket);
			InfluxDBClient influxDBClient = InfluxDBClientFactory.create(host, token.toCharArray(), org, bucket);
			Log.info("Connection established with DB");
			return influxDBClient;
		} catch (Exception e) {
			Log.error("Error connecting with Influx DB: [error: {}] [cause: {}]", e.getMessage(), e.getCause());
			e.printStackTrace();
			return null;
		}
	}

	@Bean
	public MqttClient mqttClient() {

		Log.info("Connecting with MQTT Broker...");

		String host = "tcp://localhost:1883";
		String client_id = "client";

		try {
			Log.info("Attempting connection with [host: {}]", host);

			MqttClient client = new MqttClient(host, client_id, new MemoryPersistence());
			MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

			client.setCallback(new TemperatureCallback());

			client.connect(mqttConnectOptions);

			client.subscribe("/grow-environment/temperature");
			client.subscribe("/grow-environment/humidity");
			client.subscribe("/grow-environment/co2");

			Log.info("Connection established with Broker");
			return client;
		} catch (Exception e) {
			Log.error("Error connecting with MQTT Broker: [error: {}] [cause: {}]", e.getMessage(), e.getCause());
			e.printStackTrace();
			return null;
		}
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
