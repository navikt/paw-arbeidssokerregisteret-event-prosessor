package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import java.time.Duration
import java.time.Instant
import java.util.*

fun main() {
    val kafkaKonfigurasjon = lastKonfigurasjon<KafkaKonfigurasjon>(kafkaKonfigurasjonsfil)
    val producerCfg = kafkaProducerProperties(
        producerId = "test",
        keySerializer = Serdes.String().serializer()::class,
        valueSerializer = SpecificAvroSerde<SpecificRecord>().serializer()::class
    )

    val consumer = KafkaConsumer<String, SpecificRecord>(
        kafkaKonfigurasjon.properties +
                ("key.deserializer" to Serdes.String().deserializer()::class.java.name) +
                ("value.deserializer" to SpecificAvroSerde<SpecificRecord>().deserializer()::class.java.name) +
                ("group.id" to "test")
    )

    consumer.subscribe(listOf(kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic))
    val eventerFørStart = consumer.poll(Duration.ofSeconds(1))
    consumer.commitSync()

    val producer = KafkaProducer<String, SpecificRecord>(kafkaKonfigurasjon.properties + producerCfg)

    val periodeBruker1 = UUID.randomUUID().toString()
    val periodeBruker2 = UUID.randomUUID().toString()
    val periodeBruker3 = UUID.randomUUID().toString()
    with(TestContext(producer, kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic)) {
        start(periodeBruker1)
        start(periodeBruker1)
        start(periodeBruker1)
        start(periodeBruker3)
        start(periodeBruker1)
        start(periodeBruker2)
        stop(periodeBruker2)
        stop(periodeBruker3)
        stop(periodeBruker1)
        start(periodeBruker2)
    }
    producer.flush()
    producer.close()
    Thread.sleep(5000)
    val events = consumer.poll(Duration.ofSeconds(1))
    consumer.commitSync()
    println("Antall eventer før start=${eventerFørStart.count()}")
    println("Antall eventer=${events.count()}")
    assert(events.count() == 7)
    events.forEach { println(it.value()) }
}

class TestContext(private val producer: KafkaProducer<String, SpecificRecord>, private val topic: String) {
    fun start(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id,
                Hendelse(
                    id,
                    Metadata(
                        UUID.randomUUID(),
                        Instant.now(),
                        Bruker(BrukerType.SLUTTBRUKER, "test"),
                        "unit-test",
                        "tester"),
                    Start()
                )
            )
        )
            .get()
    }

    fun stop(id: String) {
        producer.send(
            ProducerRecord(
                topic,
                id,
                Hendelse(
                    id,
                    Metadata(
                        UUID.randomUUID(),
                        Instant.now(),
                        Bruker(BrukerType.SYSTEM, "test"),
                        "unit-test",
                        "tester"),
                    Stopp()
                )
            )
        )
            .get()
    }
}