/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
