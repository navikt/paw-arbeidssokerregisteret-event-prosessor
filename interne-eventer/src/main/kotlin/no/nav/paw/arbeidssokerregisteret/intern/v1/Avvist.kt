package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.util.*

data class Avvist(
    override val hendelseId: UUID,
    override val identitetsnummer: String,
    override val metadata: Metadata
) : Hendelse {
    override val hendelseType: String = avvistHendelseType
}