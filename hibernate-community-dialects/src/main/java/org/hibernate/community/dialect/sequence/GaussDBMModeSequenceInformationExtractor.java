/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * Sequence metadata extractor for GaussDB M mode (MySQL-compatible).
 *
 * M mode has no {@code information_schema.sequences} view (MySQL has no sequences concept), so the legacy
 * extractor's {@code sequence_name}/catalog/schema/start/min/max/increment columns are unavailable. Sequences
 * are still queryable via {@code pg_class} ({@code relkind='S'}) joined with {@code pg_sequence_parameters(oid)},
 * which exposes {@code relname} and the {@code increment} value (see {@code getQuerySequencesString}).
 *
 * Returning {@code relname} plus {@code increment} lets {@link org.hibernate.tool.schema.internal.AbstractSchemaMigrator}
 * detect existing sequences (avoiding redundant {@code create sequence} calls that fail with "Relation already
 * exists" — M mode rejects {@code create sequence if not exists}) AND exposes the increment value for
 * {@code SequenceInformation.getIncrementValue()} and HHH-12973 sequence-mismatch detection. The other columns
 * stay null because {@code pg_class} does not carry them in M mode.
 *
 * A mode keeps the default extractor backed by {@code information_schema.sequences}.
 */
public class GaussDBMModeSequenceInformationExtractor extends SequenceInformationExtractorLegacyImpl {

	public static final GaussDBMModeSequenceInformationExtractor INSTANCE = new GaussDBMModeSequenceInformationExtractor();

	@Override
	protected String sequenceNameColumn() {
		return "relname";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return null;
	}

	@Override
	protected String sequenceStartValueColumn() {
		return null;
	}

	@Override
	protected String sequenceMinValueColumn() {
		return null;
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return null;
	}

	@Override
	protected String sequenceIncrementColumn() {
		// pg_sequence_parameters(oid) exposes the increment as its "increment" field; aliased to "increment"
		// in getQuerySequencesString. Non-null so SequenceInformation.getIncrementValue() works (HHH-12973).
		return "increment";
	}
}
