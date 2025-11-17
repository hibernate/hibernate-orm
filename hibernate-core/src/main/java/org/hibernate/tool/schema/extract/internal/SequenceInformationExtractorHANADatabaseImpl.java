/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorHANADatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorHANADatabaseImpl INSTANCE = new SequenceInformationExtractorHANADatabaseImpl();

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return "schema_name";
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "start_number";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "min_value";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "max_value";
	}

	@Override
	protected String sequenceIncrementColumn() {
		return "increment_by";
	}
}
