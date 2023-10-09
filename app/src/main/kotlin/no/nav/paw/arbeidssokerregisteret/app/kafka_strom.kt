package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.app.config.SchemaRegistryConfig
import no.nav.paw.arbeidssokerregisteret.app.config.helpers.konfigVerdi
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier
//Å endre denne vil ha samme effekt som å slette alle lagrede tilstander
const val PERIODE_DB_NAVN = "periode-db-v1"
fun konfigurerApplikasjon(konfigurasjon: Map<String, String>): ApplikasjonKonfigurasjon<Hendelse> {
    val schemaRegistryConfig = SchemaRegistryConfig(konfigurasjon)
    val (builder, strøm) = konfigurerKafkaStrøm(
        hendelseLog = konfigurasjon.konfigVerdi("EVENTLOG_TOPIC"),
        tilstandSerde = lagSpecificAvroSerde(schemaRegistryConfig),
        hendelseSerde = lagSpecificAvroSerde<Hendelse>(schemaRegistryConfig),
        dbNavn = PERIODE_DB_NAVN
    )
    return ApplikasjonKonfigurasjon(
        builder = builder,
        stream = strøm,
        dbNavn = PERIODE_DB_NAVN,
        tilstandSerde = lagSpecificAvroSerde(schemaRegistryConfig),
        hendelseSerde = lagSpecificAvroSerde(schemaRegistryConfig)
    )
}
fun <T> konfigurerKafkaStrøm(
    hendelseLog: String,
    tilstandSerde: Serde<PeriodeTilstandV1>,
    hendelseSerde: Serde<T>,
    dbNavn: String,
): Pair<StreamsBuilder, KStream<String, T>> {
    val builder = StreamsBuilder().addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
            Serdes.String(),
            tilstandSerde,
            Time.SYSTEM
        )
    )
    val stream  = builder.stream(
        hendelseLog, Consumed.with(
            Serdes.String(),
            hendelseSerde
        )
    )
    return builder to stream
}

data class ApplikasjonKonfigurasjon<T>(
    val builder: StreamsBuilder,
    val stream: KStream<String, T>,
    val dbNavn: String,
    val tilstandSerde: Serde<PeriodeTilstandV1>,
    val hendelseSerde: Serde<T>
)
