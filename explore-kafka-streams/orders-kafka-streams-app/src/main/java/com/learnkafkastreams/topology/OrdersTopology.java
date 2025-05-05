package com.learnkafkastreams.topology;

import com.learnkafkastreams.domain.Order;
import com.learnkafkastreams.domain.OrderType;
import com.learnkafkastreams.domain.Revenue;
import com.learnkafkastreams.serdes.SerdesFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

@Slf4j
public class OrdersTopology {
    public static final String ORDERS = "orders";
    public static final String RESTAURANT_ORDERS = "restaurant_orders";
    public static final String GENERAL_ORDERS = "general_orders";
    public static final String STORES = "stores";


    public static Topology buildTopology() {
        Predicate<String, Order> generalPredicate = (key, order) -> order.orderType().equals(OrderType.GENERAL);
        Predicate<String, Order> restaurantPredicate = (key, order) -> order.orderType().equals(OrderType.RESTAURANT);

        ValueMapper<Order, Revenue> revenueMapper = order -> new Revenue(order.locationId(), order.finalAmount());

        StreamsBuilder streamsBuilder = new StreamsBuilder();

        // Original Stream
        var orderStream = streamsBuilder
                .stream(ORDERS,
                        Consumed.with(Serdes.String(), SerdesFactory.orderSerdes())
                );

        orderStream.print(Printed.<String, Order>toSysOut().withLabel("orders"));

        // Split and Branch Original Stream - branch into two different streams and puhlish to two different topics altogheter
        orderStream
                .split(Named.as("General-restaurant-stream"))
                .branch(generalPredicate,
                        Branched.withConsumer(generalOrderStream -> {
                            generalOrderStream.print(Printed.<String, Order>toSysOut().withLabel("generalStream"));  // code for debugging

                            generalOrderStream
                                    .mapValues((readOnlyKey, value) -> revenueMapper.apply(value))
                                    .to(GENERAL_ORDERS,  //publishing to GENERAL_ORDERS Topic
                                            Produced.with(Serdes.String(), SerdesFactory.revenueSerdes()));
                        })
                )
                .branch(restaurantPredicate,
                        Branched.withConsumer(restaurantOrderStream -> {
                            restaurantOrderStream.print(Printed.<String, Order>toSysOut().withLabel("restaurantStream")); // code for debugging
                            restaurantOrderStream
                                    .mapValues((readOnlyKey, value) -> revenueMapper.apply(value))
                                    .to(RESTAURANT_ORDERS,  //publishing to RESTAURANT_ORDERS Topic
                                            Produced.with(Serdes.String(), SerdesFactory.revenueSerdes()));
                        })
                );

        return streamsBuilder.build();
    }
}
