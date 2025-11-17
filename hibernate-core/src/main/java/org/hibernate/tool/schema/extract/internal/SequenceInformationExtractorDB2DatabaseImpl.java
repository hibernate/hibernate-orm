/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorDB2DatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorDB2DatabaseImpl INSTANCE = new SequenceInformationExtractorDB2DatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "seqname";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return "seqschema";
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "start";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "minvalue";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "maxvalue";
	}
}
