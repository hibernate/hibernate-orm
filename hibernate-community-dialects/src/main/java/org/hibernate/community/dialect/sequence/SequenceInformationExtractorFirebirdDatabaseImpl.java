/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Mark Rotteveel
 */
public class SequenceInformationExtractorFirebirdDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorFirebirdDatabaseImpl INSTANCE = new SequenceInformationExtractorFirebirdDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "rdb$generator_name";
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
		return "rdb$initial_value";
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
		return "rdb$generator_increment";
	}
}
