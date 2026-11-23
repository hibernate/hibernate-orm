package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

/**
 * Sequence metadata extractor for GaussDB M mode (MySQL-compatible).
 *
 * M mode has no {@code information_schema.sequences} view (MySQL has no sequences concept), so the standard
 * extractor's catalog/schema/start/min/max/increment columns are unavailable. Sequences are still queryable
 * via {@code pg_class} ({@code relkind='S'}) joined with {@code pg_sequence_parameters(oid)}, which exposes
 * {@code relname} and the {@code increment} value (see {@code GaussDBDialect#getQuerySequencesString},
 * which renders that lookup SQL).
 *
 * Returning {@code relname} plus {@code increment} lets {@link org.hibernate.tool.schema.internal.AbstractSchemaMigrator}
 * detect existing sequences (avoiding redundant {@code create sequence} calls that fail with "Relation already
 * exists" — M mode rejects {@code create sequence if not exists}) AND exposes the increment value for
 * {@link SequenceInformationExtractor}'s SequenceInformation#getIncrementValue() and HHH-12973
 * sequence-mismatch detection. The other columns stay null because {@code pg_class} does not carry them
 * in M mode.
 *
 * A mode keeps the default extractor backed by {@code information_schema.sequences}.
 */
public final class GaussDBMModeSequenceInformationExtractor {

	public static final SequenceInformationExtractor INSTANCE =
			SequenceInformationExtractors.builder( "select c.relname as relname, (pg_sequence_parameters(c.oid)).increment as increment from pg_class c where c.relkind='S'" )
					.withoutCatalog()
					.withoutSchema()
					// pg_sequence_parameters(oid) exposes the increment as its "increment" field; aliased to
					// "increment" in the lookup SQL above. Non-null so SequenceInformation.getIncrementValue()
					// works (HHH-12973).
					.incrementValueColumn( "increment" )
					.build();

	private GaussDBMModeSequenceInformationExtractor() {
	}
}
