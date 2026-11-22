package com.pedidos360.backend.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String TOPIC_PEDIDOS_EVENTOS = "pedidos.eventos";
    public static final String TOPIC_INVENTARIO_EVENTOS = "inventario.eventos";
    public static final String TOPIC_PEDIDOS_DLT = "pedidos.eventos.dlt";
    public static final String TOPIC_INVENTARIO_DLT = "inventario.eventos.dlt";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // 1. ProducerFactory y KafkaTemplate explícitos (Spring Boot 4)
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // 2. ConsumerFactory y ContainerFactory con ErrorHandlingDeserializer + DLT
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

@Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition())
        );

        org.springframework.util.backoff.BackOff backOff = () -> new org.springframework.util.backoff.BackOffExecution() {
            private int attempts = 0;

            @Override
            public long nextBackOff() {
                return (++attempts <= 3) ? 2000L : STOP;
            }
        };

        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    // 3. KafkaAdmin para que Spring cree los NewTopic automáticamente al arrancar
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // 4. Tópicos principales y Dead Letter Topics (3 particiones, RF=3, retención)
    @Bean
    public NewTopic pedidosEventosTopic() {
        return TopicBuilder.name(TOPIC_PEDIDOS_EVENTOS)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic inventarioEventosTopic() {
        return TopicBuilder.name(TOPIC_INVENTARIO_EVENTOS)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.RETENTION_MS_CONFIG, "259200000")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic pedidosDltTopic() {
        return TopicBuilder.name(TOPIC_PEDIDOS_DLT)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.RETENTION_MS_CONFIG, "1209600000")
                .build();
    }

    @Bean
    public NewTopic inventarioDltTopic() {
        return TopicBuilder.name(TOPIC_INVENTARIO_DLT)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.RETENTION_MS_CONFIG, "1209600000")
                .build();
    }

    // 5. Cliente de Administración Kafka para endpoints REST
    @Bean
    public AdminClient kafkaAdminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(configs);
    }
}