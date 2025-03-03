/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorIngresDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorIngresDatabaseImpl INSTANCE = new SequenceInformationExtractorIngresDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "seq_name";
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
		return null;
	}
}
