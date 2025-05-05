package com.learnkafkastreams.launcher;

import com.learnkafkastreams.topology.GreetingsTopology;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class GreetingsStreamApp {

    public static void main(String[] args) {

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "greetings-app");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put("allow.auto.create.topics", "false");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());

        //createTopics(properties, List.of(GreetingsTopology.GREETINGS_SPANISH, GreetingsTopology.GREETINGS, GreetingsTopology.GREETINGS_UPPERCASE));
        var greetingsTopology =GreetingsTopology.buildTopology();

        // Executes the Topology
        var kafkaStream = new KafkaStreams(greetingsTopology, properties);

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStream::close));
        try {
            kafkaStream.start();
        }catch (Exception e) {
            log.error("Exception in starting the Stream : {}", e.getMessage());
        }


    }

    /**
     *
     * @param config broker data that you want to have this appliation
     * @param greetings List of topics we want
     */
    private static void createTopics(Properties config, List<String> greetings) {

        AdminClient admin = AdminClient.create(config);
        var partitions = 2;
        short replication  = 1;

        var newTopics = greetings
                .stream()
                .map(topic ->{
                    return new NewTopic(topic, partitions, replication);
                })
                .collect(Collectors.toList());

        var createTopicResult = admin.createTopics(newTopics);
        try {
           createTopicResult
                    .all().get();
            log.info("topics are created successfully");
        } catch (Exception e) {
            log.error("Exception creating topics : {} ",e.getMessage(), e);
        }
    }
}
