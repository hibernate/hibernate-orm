/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorMimerSQLDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorMimerSQLDatabaseImpl INSTANCE = new SequenceInformationExtractorMimerSQLDatabaseImpl();

	@Override
	protected String sequenceStartValueColumn() {
		return "initial_value";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return null;
	}
}
