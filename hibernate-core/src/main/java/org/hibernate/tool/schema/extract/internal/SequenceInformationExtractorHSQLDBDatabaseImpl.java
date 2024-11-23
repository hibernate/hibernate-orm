/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorHSQLDBDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorHSQLDBDatabaseImpl INSTANCE = new SequenceInformationExtractorHSQLDBDatabaseImpl();

	@Override
	protected String sequenceStartValueColumn() {
		return "start_with";
	}

}
