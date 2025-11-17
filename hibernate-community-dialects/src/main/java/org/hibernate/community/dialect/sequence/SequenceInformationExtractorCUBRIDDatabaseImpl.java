/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorCUBRIDDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorCUBRIDDatabaseImpl INSTANCE = new SequenceInformationExtractorCUBRIDDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "name";
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
		return "started";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "min_val";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "max_val";
	}

	@Override
	protected String sequenceIncrementColumn() {
		return "increment_val";
	}
}
