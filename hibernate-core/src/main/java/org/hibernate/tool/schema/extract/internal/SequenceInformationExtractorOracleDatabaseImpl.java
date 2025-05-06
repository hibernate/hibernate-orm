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
public class SequenceInformationExtractorOracleDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorOracleDatabaseImpl INSTANCE = new SequenceInformationExtractorOracleDatabaseImpl();

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
		return "min_value";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "max_value";
	}


	@Override
	protected Number resultSetIncrementValue(ResultSet resultSet) throws SQLException {
		return resultSet.getBigDecimal( sequenceIncrementColumn() );
	}

	@Override
	protected Number resultSetMinValue(ResultSet resultSet) throws SQLException {
		return resultSet.getBigDecimal( sequenceMinValueColumn() );
	}

	@Override
	protected Number resultSetMaxValue(ResultSet resultSet) throws SQLException {
		return resultSet.getBigDecimal( sequenceMaxValueColumn() );
	}

	@Override
	protected String sequenceIncrementColumn() {
		return "increment_by";
	}
}
