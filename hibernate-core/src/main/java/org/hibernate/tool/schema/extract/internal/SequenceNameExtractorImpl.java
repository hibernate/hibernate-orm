/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
public class SequenceNameExtractorImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceNameExtractorImpl INSTANCE = new SequenceNameExtractorImpl();

	protected String resultSetSequenceName(ResultSet resultSet) throws SQLException {
		return resultSet.getString( 1 );
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
