import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;

import java.util.Arrays;
import java.util.Properties;

import io.gamov.wordvount.util.WordUtil;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.*;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

public class StreamsStarterApp {

  public static void main(String[] args) {
    Properties config = new Properties();
    config.put(APPLICATION_ID_CONFIG, "wordcount-app");
    config.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    config.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    config.put(CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
    KStreamBuilder builder = new KStreamBuilder();

    KStream<String, String> stream = builder
        .stream("word-count-input");

    KTable<String, Long> counts = stream
        .flatMapValues(v -> Arrays.asList(WordUtil.PATTERN.split(v)))
        .mapValues(String::toLowerCase)
        .mapValues(WordUtil::cleanWord)
        .filter((key, value) -> value.length()>=5)
        .selectKey((k, word) -> word)
        .groupByKey()
        .count("Counts");

    counts.to(Serdes.String(), Serdes.Long(), "word-count-output");

    KafkaStreams kafkaStreams = new KafkaStreams(builder, config);
    kafkaStreams.start();
    System.out.println(kafkaStreams.toString());
    Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
  }

}
