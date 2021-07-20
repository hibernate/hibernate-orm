/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.math.BigDecimal;
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
	protected Long resultSetMinValue(ResultSet resultSet) throws SQLException {
		BigDecimal asDecimal = resultSet.getBigDecimal( sequenceMinValueColumn() );

		// BigDecimal.longValue() may return a result with the opposite sign
		if ( asDecimal.compareTo( BigDecimal.valueOf( Long.MIN_VALUE ) ) == -1 ) {
			return Long.MIN_VALUE;
		}

		return asDecimal.longValue();
	}

	@Override
	protected Long resultSetMaxValue(ResultSet resultSet) throws SQLException {
		BigDecimal asDecimal = resultSet.getBigDecimal( sequenceMaxValueColumn() );

		// BigDecimal.longValue() may return a result with the opposite sign
		if ( asDecimal.compareTo( BigDecimal.valueOf( Long.MAX_VALUE ) ) == 1 ) {
			return Long.MAX_VALUE;
		}

		return asDecimal.longValue();
	}

	@Override
	protected String sequenceIncrementColumn() {
		return "increment_by";
	}
}
