package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.applications.alarm.model.AlarmTreePath;

public class AlarmCommandLogger implements Runnable {

    private final String topic;
    private Map<String, Object> serdeProps;
    private final Serde<AlarmCommandMessage> alarmCommandMessageSerde;
    
    private final Pattern pattern = Pattern.compile("(\\w*://\\S*)");

    public AlarmCommandLogger(String topic) {
        super();
        this.topic = topic;
        MessageParser<AlarmCommandMessage> messageParser = new MessageParser<AlarmCommandMessage>(AlarmCommandMessage.class);
        alarmCommandMessageSerde = Serdes.serdeFrom(messageParser, messageParser);
    }

    @Override
    public void run() {
        logger.info("Starting the stream consumer");

        Properties props = PropertiesHelper.getProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-"+topic+"-alarm-command");
        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, AlarmCommandMessage> alarms = builder.stream(topic+"Command",
                Consumed.with(Serdes.String(), alarmCommandMessageSerde)
                        .withTimestampExtractor(new TimestampExtractor() {
                            
                            @Override
                            public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
                                return record.timestamp();
                            }
                        }));

        // transform the alarm messages, include the pv and config path
        // create store
        StoreBuilder<KeyValueStore<String,AlarmCommandMessage>> keyValueStoreBuilder =
                Stores.keyValueStoreBuilder(Stores.lruMap(topic+"_cmd_state_store", 100),
                        Serdes.String(),
                        alarmCommandMessageSerde);
        // register store
        builder.addStateStore(keyValueStoreBuilder);

        KStream<String, AlarmCommandMessage> transformedAlarms = alarms.transform(new TransformerSupplier<String, AlarmCommandMessage, KeyValue<String,AlarmCommandMessage>>() {

            @Override
            public Transformer<String, AlarmCommandMessage, KeyValue<String, AlarmCommandMessage>> get() {
                return new Transformer<String, AlarmCommandMessage, KeyValue<String, AlarmCommandMessage>>() {
                    private ProcessorContext context;
                    private StateStore state;

                    @Override
                    public KeyValue<String, AlarmCommandMessage> transform(String key, AlarmCommandMessage value) {
                        key = key.replace("\\", "");
                        Matcher matcher = pattern.matcher(key);
                        value.setConfig(key);
                        matcher.find();
                        String[] tokens = AlarmTreePath.splitPath(key);
                        value.setPv(tokens[tokens.length - 1]);
                        value.setMessage_time(Instant.ofEpochMilli(context.timestamp()));
                        return new KeyValue<String, AlarmCommandMessage>(key, value);
                    }

                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                        this.state = context.getStateStore(topic+"_cmd_state_store");
                    }

                    @Override
                    public KeyValue<String, AlarmCommandMessage> punctuate(long timestamp) {
                        return null;
                    }

                    @Override
                    public void close() {
                        // TODO Auto-generated method stub
                        
                    }
                };
            }
        }, topic+"_cmd_state_store");

        // Commit to elastic
        transformedAlarms.foreach((k, v) -> {
            ElasticClientHelper.getInstance().indexAlarmCommandDocument(topic + "_alarms", v);
        });
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-"+topic+"-alarm-cmd-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

}
