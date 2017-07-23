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

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.select.ResultSetAccess;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.exec.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Implementation of SqlSelection for native-SQL queries
 * adding "auto discovery" capabilities.
 *
 * @author Steve Ebersole
 */
public class SqlSelectionImpl implements SqlSelection, SqlSelectable, SqlSelectionReader {
	private final String columnAlias;
	private ValueExtractor extractor;

	private Integer jdbcResultSetPosition;

	public SqlSelectionImpl(String columnAlias, int jdbcResultSetPosition) {
		this.columnAlias = columnAlias;
		this.jdbcResultSetPosition = jdbcResultSetPosition;
	}

	public SqlSelectionImpl(String columnAlias) {
		this( columnAlias, null );
	}

	public SqlSelectionImpl(String columnAlias, ValueExtractor extractor) {
		this.columnAlias = columnAlias;
		this.extractor = extractor;
	}

	public ValueExtractor getExtractor() {
		return extractor;
	}

	@Override
	public void prepare(ResultSetAccess resultSetAccess, SharedSessionContractImplementor persistenceContext) {
		// resolve the column-alias to a position
		jdbcResultSetPosition = resultSetAccess.resolveColumnPosition( columnAlias );

		if ( extractor == null ) {
			// assume we should auto-discover the type
			final SqlTypeDescriptor sqlTypeDescriptor = resultSetAccess.resolveSqlTypeDescriptor( jdbcResultSetPosition );

			extractor = sqlTypeDescriptor.getExtractor(
					sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( persistenceContext.getFactory().getTypeConfiguration() )
			);
		}

	}

	@Override
	public SqlSelectable getSqlSelectable() {
		return this;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return this;
	}

	@Override
	public Object read(
			ResultSet resultSet,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			SqlSelection sqlSelection) throws SQLException {
		validateExtractor();

		return extractor.extract(
				resultSet,
				sqlSelection.getJdbcResultSetIndex(),
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	private void validateExtractor() {
		if ( extractor == null ) {
			throw new QueryException( "Could not determine how to read JDBC value" );
		}
	}

	@Override
	public Object extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			int jdbcParameterIndex) throws SQLException {
		validateExtractor();

		return extractor.extract(
				statement,
				jdbcParameterIndex,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	public Object extractParameterValue(
			CallableStatement statement,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			String jdbcParameterName) throws SQLException {
		validateExtractor();

		return extractor.extract(
				statement,
				jdbcParameterName,
				jdbcValuesSourceProcessingState.getPersistenceContext()
		);
	}

	@Override
	public int getJdbcResultSetIndex() {
		return jdbcResultSetPosition;
	}

	@Override
	public int getValuesArrayPosition() {
		return jdbcResultSetPosition -1;
	}

	@Override
	public void accept(SqlAstWalker interpreter) {

	}
}
