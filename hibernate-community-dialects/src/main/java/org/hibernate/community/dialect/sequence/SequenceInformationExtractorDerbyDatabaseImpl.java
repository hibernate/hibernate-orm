/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorDerbyDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorDerbyDatabaseImpl INSTANCE = new SequenceInformationExtractorDerbyDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "sequencename";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "startvalue";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "minimumvalue";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "maximumvalue";
	}
}
