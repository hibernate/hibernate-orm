/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorInformixDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorInformixDatabaseImpl INSTANCE = new SequenceInformationExtractorInformixDatabaseImpl();

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
		return "start_val";
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
		return "inc_val";
	}
}
