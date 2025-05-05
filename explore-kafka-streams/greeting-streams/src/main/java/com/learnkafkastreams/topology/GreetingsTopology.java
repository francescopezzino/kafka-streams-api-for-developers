package com.learnkafkastreams.topology;

import com.learnkafkastreams.domain.Greeting;
import com.learnkafkastreams.serdes.SerdesFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class GreetingsTopology {

    // source topic
    public static String GREETINGS = "greetings";

    // destination topic
    public static String GREETINGS_UPPERCASE = "greetings_uppercase";

    public static String GREETINGS_SPANISH = "greetings_spanish";

    public static Topology buildTopology() {

        // to perform the pipeline
        StreamsBuilder streamsBuilder = new StreamsBuilder();

//        KStream<String,String> mergedStream = getStringGreetingKStream(streamsBuilder);

        var mergedStream = getCustomGreetingKStream(streamsBuilder);

        mergedStream
                //.print(Printed.<String, String>toSysOut().withLabel("mergedStream"));
                .print(Printed.<String, Greeting>toSysOut().withLabel("mergedStream"));

        // Performing the data Enrichment
        var modifiedStream = mergedStream
//                .filter((key, value) -> value.length() > 5 )
//                .peek((key, value) -> {
//                    log.info("After filter : key {}, value : {}", key, value);
//                })
                 .mapValues((readOnlyKey, value) -> new Greeting(value.message().toUpperCase(), value.timeStamp())
                 )
//                .peek((key, value) -> {
//                    log.info("After mapValues : key {}, value : {}", key, value);
//                })
//                 .map((key, value) -> KeyValue.pair(key.toUpperCase(), value.toUpperCase()))
//                .flatMap((key, value) -> {
//                    var newValues = Arrays.asList(value.split(""));
//                    return newValues
//                            .stream()
//                            .map(val -> KeyValue.pair(key, val.toUpperCase()))
//                            .collect(Collectors.toList());
//                });
//                .flatMapValues((key, value) -> {
//                    var newValues = Arrays.asList(value.split(""));
//                    return newValues
//                            .stream()
//                            .map(String::toUpperCase)
//                            .collect(Collectors.toList());
//                })
        ;



        modifiedStream
                //.print(Printed.<String, String>toSysOut().withLabel("modifiedStream"));
                .print(Printed.<String, Greeting>toSysOut().withLabel("modifiedStream"));
        // Writing back to the Kafka topic using the to operator
        modifiedStream
                .to(GREETINGS_UPPERCASE
                        ,
                        Produced.with(Serdes.String(), SerdesFactory.greetingSerdesUsingGenerics())
                );

        return streamsBuilder.build(); // returns the topology

    }

    private static KStream<String, Greeting> getCustomGreetingKStream(StreamsBuilder streamsBuilder) {
        // Reading from the Kafka topic
       var greetingStream = streamsBuilder
                .stream(GREETINGS, Consumed.with(Serdes.String(), SerdesFactory.greetingSerdesUsingGenerics()));

        var greetingsSpanishStream = streamsBuilder
                .stream(GREETINGS_SPANISH, Consumed.with(Serdes.String(), SerdesFactory.greetingSerdesUsingGenerics()));

        var mergedStream = greetingStream.merge(greetingsSpanishStream);
        return mergedStream;
    }

    private static KStream<String, String> getStringGreetingKStream(StreamsBuilder streamsBuilder) {
        // Reading from the Kafka topic
        KStream<String, String> greetingStream = streamsBuilder
                .stream(GREETINGS
//                        , Consumed.with(Serdes.String(), Serdes.String())
                );

        KStream<String, String> greetingsSpanishStream = streamsBuilder
                .stream(GREETINGS_SPANISH
//                        , Consumed.with(Serdes.String(), Serdes.String())
                );

        var mergedStream = greetingStream.merge(greetingsSpanishStream);
        return mergedStream;
    }
}
