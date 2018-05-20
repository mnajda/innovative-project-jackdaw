package com.jackdaw.producer;

import com.jackdaw.avro.flights.Flight;
import com.jackdaw.avro.flights.FlightSituation;
import com.jackdaw.avro.flights.FlightType;
import com.jackdaw.avro.flights.TimeType;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

public class FlightDataKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(FlightDataKafkaProducer.class);

    private final String topicName;
    private final Producer<Long, Flight> producer;
    private final String inputFileName;

    public FlightDataKafkaProducer(String inputFileName, String topicName, Producer<Long, Flight> producer) throws IOException {
        this.producer = producer;
        this.inputFileName = inputFileName;
        this.topicName = topicName;
    }

    public void runProducer() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/volume/" + inputFileName)))) {
            runProducer(br);
        } catch (IOException e) {
            LOG.error("Failed to open file {}", inputFileName, e);
        }
    }

    void runProducer(BufferedReader br) {
        long lineCount = 0;
        try {
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] splitMessage = line.split(",");
                if (splitMessage[5].equals("Realizado")) {
                    this.sendMessage(lineCount, createFlight(splitMessage));
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void sendMessage(Long key, Flight value) {
        try {
            producer.send(new ProducerRecord<>(topicName, key, value)).get();
            LOG.info("Sent message: ({}, {})", key, value);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("", e);
        }
    }

    Flight createFlight(String[] splitMessage) {
        if (splitMessage.length != 19) {
            throw new IllegalArgumentException("Array size different than 21, data is corrupted");
        } else {
            Flight record = new Flight();
            int index = 0;
            for (String data : splitMessage) {
                if (index == 2) {
                    record.put(index, FlightType.valueOf(data));
                } else if (index == 3) {
                    record.put(index, TimeType.valueOf(data));
                } else if (index == 5) {
                    record.put(index, FlightSituation.valueOf(data));
                } else if (index >= 15) {
                    record.put(index, Double.parseDouble(data));
                } else {
                    record.put(index, data);
                }
                ++index;
            }
            return record;
        }
    }


}
