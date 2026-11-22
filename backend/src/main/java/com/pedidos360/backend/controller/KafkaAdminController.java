package com.pedidos360.backend.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pedidos360.backend.dto.InventarioKafkaEvent;
import com.pedidos360.backend.dto.PedidoKafkaEvent;
import com.pedidos360.backend.kafka.KafkaEventConsumers;
import com.pedidos360.backend.kafka.KafkaEventProducer;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final AdminClient adminClient;
    private final KafkaEventProducer producer;
    private final KafkaEventConsumers consumers;

    public KafkaAdminController(AdminClient adminClient, KafkaEventProducer producer, KafkaEventConsumers consumers) {
        this.adminClient = adminClient;
        this.producer = producer;
        this.consumers = consumers;
    }

    @GetMapping("/topics")
    public List<Object> listarTopicos() throws ExecutionException, InterruptedException {
        Set<String> nombres = adminClient.listTopics().names().get().stream()
                .filter(name -> !name.startsWith("__"))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (nombres.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, TopicDescription> descripciones = adminClient.describeTopics(nombres).allTopicNames().get();

        List<ConfigResource> recursos = nombres.stream()
                .map(n -> new ConfigResource(ConfigResource.Type.TOPIC, n))
                .collect(Collectors.toList());
        Map<ConfigResource, Config> configs = adminClient.describeConfigs(recursos).all().get();

        List<Object> resultado = new ArrayList<>();
        for (String nombre : nombres) {
            TopicDescription desc = descripciones.get(nombre);
            Config cfg = configs.get(new ConfigResource(ConfigResource.Type.TOPIC, nombre));
            String retentionMs = cfg != null && cfg.get(TopicConfig.RETENTION_MS_CONFIG) != null
                    ? cfg.get(TopicConfig.RETENTION_MS_CONFIG).value()
                    : "604800000";

            int particiones = desc.partitions().size();
            int rf = particiones > 0 ? desc.partitions().get(0).replicas().size() : 0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", nombre);
            item.put("partitions", particiones);
            item.put("replicationFactor", rf);
            item.put("retentionMs", retentionMs);
            resultado.add(item);
        }
        return resultado;
    }

    @PostMapping("/topics")
    public Map<String, String> crearTopico(@RequestBody Map<String, Object> body) {
        try {
            String name = String.valueOf(body.get("name")).trim();
            int partitions = Integer.parseInt(String.valueOf(body.getOrDefault("partitions", 3)));
            short replicationFactor = Short.parseShort(String.valueOf(body.getOrDefault("replicationFactor", 3)));
            String retentionMs = String.valueOf(body.getOrDefault("retentionMs", "604800000"));

            NewTopic nuevo = new NewTopic(name, partitions, replicationFactor)
                    .configs(Map.of(
                            TopicConfig.RETENTION_MS_CONFIG, retentionMs,
                            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(Math.min(2, replicationFactor))
                    ));

            adminClient.createTopics(Collections.singletonList(nuevo)).all().get();
            return Map.of("mensaje", "Tópico '" + name + "' creado correctamente");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al crear tópico: " + e.getMessage());
        }
    }

    @PutMapping("/topics/{name}")
    public Map<String, String> actualizarTopico(@PathVariable String name, @RequestBody Map<String, Object> body) {
        try {
            if (body.containsKey("partitions") && body.get("partitions") != null) {
                int nuevasParticiones = Integer.parseInt(String.valueOf(body.get("partitions")));
                TopicDescription actual = adminClient.describeTopics(Collections.singletonList(name))
                        .allTopicNames().get().get(name);
                if (nuevasParticiones > actual.partitions().size()) {
                    adminClient.createPartitions(Map.of(name, NewPartitions.increaseTo(nuevasParticiones))).all().get();
                }
            }

            if (body.containsKey("retentionMs") && body.get("retentionMs") != null) {
                String retentionMs = String.valueOf(body.get("retentionMs"));
                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
                AlterConfigOp op = new AlterConfigOp(
                        new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, retentionMs),
                        AlterConfigOp.OpType.SET
                );
                adminClient.incrementalAlterConfigs(Map.of(resource, Collections.singletonList(op))).all().get();
            }

            return Map.of("mensaje", "Tópico '" + name + "' actualizado");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al modificar tópico: " + e.getMessage());
        }
    }

    @DeleteMapping("/topics/{name}")
    public Map<String, String> eliminarTopico(@PathVariable String name) {
        try {
            adminClient.deleteTopics(Collections.singletonList(name)).all().get();
            return Map.of("mensaje", "Tópico '" + name + "' eliminado");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al eliminar tópico: " + e.getMessage());
        }
    }

    @GetMapping("/consumer-groups")
    public List<Object> listarConsumerGroups() throws ExecutionException, InterruptedException {
        Collection<ConsumerGroupListing> grupos = adminClient.listConsumerGroups().all().get();
        List<Object> resultado = new ArrayList<>();

        for (ConsumerGroupListing g : grupos) {
            String groupId = g.groupId();
            Map<TopicPartition, OffsetAndMetadata> committed =
                    adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();

            Map<TopicPartition, OffsetSpec> reqLatest = new HashMap<>();
            for (TopicPartition tp : committed.keySet()) {
                reqLatest.put(tp, OffsetSpec.latest());
            }
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    reqLatest.isEmpty() ? Collections.emptyMap() : adminClient.listOffsets(reqLatest).all().get();

            long totalLag = 0;
            List<Object> particionesDetalle = new ArrayList<>();
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committed.entrySet()) {
                TopicPartition tp = entry.getKey();
                long currentOffset = entry.getValue() != null ? entry.getValue().offset() : 0L;
                long logEndOffset = endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : currentOffset;
                long lag = Math.max(0L, logEndOffset - currentOffset);
                totalLag += lag;

                Map<String, Object> pInfo = new LinkedHashMap<>();
                pInfo.put("topic", tp.topic());
                pInfo.put("partition", tp.partition());
                pInfo.put("currentOffset", currentOffset);
                pInfo.put("logEndOffset", logEndOffset);
                pInfo.put("lag", lag);
                particionesDetalle.add(pInfo);
            }

            Map<String, Object> groupInfo = new LinkedHashMap<>();
            groupInfo.put("groupId", groupId);
            groupInfo.put("state", g.state().map(Enum::name).orElse("STABLE"));
            groupInfo.put("totalLag", totalLag);
            groupInfo.put("partitions", particionesDetalle);
            resultado.add(groupInfo);
        }
        return resultado;
    }

    @GetMapping("/events-log")
    public List<String> obtenerLogEventos() {
        return consumers.getHistorialEventos();
    }

    @PostMapping("/test-event")
    public Map<String, String> publicarEventoPrueba(@RequestBody Map<String, Object> body) {
        String canal = String.valueOf(body.getOrDefault("canal", "PEDIDOS"));
        boolean simularFallo = Boolean.parseBoolean(String.valueOf(body.getOrDefault("simularFallo", false)));

        if ("INVENTARIO".equalsIgnoreCase(canal)) {
            producer.publicarEventoInventario(new InventarioKafkaEvent(
                    simularFallo ? "TEST_FALLO_DLT" : "TEST_STOCK_EVENTO",
                    999L,
                    "Producto Prueba Kafka",
                    10,
                    simularFallo ? 0 : 4,
                    simularFallo
            ));
        } else {
            producer.publicarEventoPedido(new PedidoKafkaEvent(
                    simularFallo ? "TEST_FALLO_DLT" : "TEST_PEDIDO_EVENTO",
                    999L,
                    "cliente-prueba@duocuc.cl",
                    simularFallo ? "ERROR_SIMULADO" : "PROCESADO_KAFKA",
                    25990.0,
                    simularFallo
            ));
        }

        return Map.of("mensaje", simularFallo
                ? "Evento con fallo simulado enviado -> tras 3 reintentos irá al .dlt"
                : "Evento de prueba publicado correctamente en Kafka");
    }
}