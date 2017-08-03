/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi.ast;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.type.descriptor.spi.ValueExtractor;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionReaderImpl implements SqlSelectionReader {
	@Override
	public Object read(
			ResultSet resultSet,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			SqlSelection sqlSelection) throws SQLException {
		assert sqlSelection instanceof SqlSelectionImpl;

		return getExtractor( sqlSelection ).extract(
				resultSet,
				sqlSelection.getJdbcResultSetIndex(),
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	private ValueExtractor getExtractor(SqlSelection sqlSelection) {
		assert sqlSelection instanceof SqlSelectionImpl;

		return ( (SqlSelectionImpl) sqlSelection ).getExtractor();
	}

	@Override
	public Object extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int jdbcParameterIndex) throws SQLException {
		throw new NotYetImplementedException(  );
//		return extractor.extract(
//				statement,
//				jdbcParameterIndex,
//				jdbcValuesSourceProcessingState.getPersistenceContext()
//		);
	}

	@Override
	public Object extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			String jdbcParameterName) throws SQLException {
		throw new NotYetImplementedException(  );
//		return extractor.extract(
//				statement,
//				jdbcParameterName,
//				jdbcValuesSourceProcessingState.getPersistenceContext()
//		);
	}
}
