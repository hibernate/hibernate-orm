/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Implementation of SqlSelection for native-SQL queries
 * adding "auto discovery" capabilities.
 *
 * @author Steve Ebersole
 */
public class ResolvingSqlSelectionImpl implements SqlSelection, JdbcValueExtractor {
	private final String columnAlias;
	private SqlExpressableType sqlExpressableType;

	private Integer jdbcResultSetPosition;

	@SuppressWarnings("unused")
	public ResolvingSqlSelectionImpl(String columnAlias, int jdbcResultSetPosition) {
		this.columnAlias = columnAlias;
		this.jdbcResultSetPosition = jdbcResultSetPosition;
	}

	@SuppressWarnings("WeakerAccess")
	public ResolvingSqlSelectionImpl(String columnAlias) {
		this( columnAlias, null );
	}

	@SuppressWarnings("WeakerAccess")
	public ResolvingSqlSelectionImpl(String columnAlias, SqlExpressableType sqlExpressableType) {
		this.columnAlias = columnAlias;
		this.sqlExpressableType = sqlExpressableType;
	}

	@SuppressWarnings("WeakerAccess")
	public ResolvingSqlSelectionImpl(int jdbcResultSetPosition, SqlExpressableType sqlExpressableType) {
		this.columnAlias = null;

		this.jdbcResultSetPosition = jdbcResultSetPosition;
		this.sqlExpressableType = sqlExpressableType;
	}

	@Override
	public void prepare(
			ResultSetMappingDescriptor.JdbcValuesMetadata jdbcResultsMetadata,
			SessionFactoryImplementor sessionFactory) {
		if ( jdbcResultSetPosition == null ) {
			// resolve the column-alias to a position
			jdbcResultSetPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
		}

		if ( sqlExpressableType == null ) {
			// assume we should auto-discover the type
			final SqlTypeDescriptor sqlTypeDescriptor = jdbcResultsMetadata.resolveSqlTypeDescriptor( jdbcResultSetPosition );

			final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
			sqlExpressableType = sqlTypeDescriptor.getSqlExpressableType(
					sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( typeConfiguration ),
					typeConfiguration
			);
		}

	}

	@Override
	public JdbcValueExtractor getJdbcValueExtractor() {
		return this;
	}

	@Override
	public Object extract(ResultSet resultSet, int position, ExecutionContext executionContext) throws SQLException {
		validateExtractor();

		return sqlExpressableType.getJdbcValueExtractor().extract(
				resultSet,
				position,
				executionContext
		);
	}

	private void validateExtractor() {
		if ( sqlExpressableType == null ) {
			throw new QueryException( "Could not determine how to read JDBC value" );
		}
	}

	@Override
	public Object extract(CallableStatement statement, int jdbcParameterPosition, ExecutionContext executionContext) throws SQLException {
		validateExtractor();

		return sqlExpressableType.getJdbcValueExtractor().extract(
				statement,
				jdbcParameterPosition,
				executionContext
		);
	}

	@Override
	public Object extract(CallableStatement statement, String jdbcParameterName, ExecutionContext executionContext) throws SQLException {
		validateExtractor();

		return sqlExpressableType.getJdbcValueExtractor().extract(
				statement,
				jdbcParameterName,
				executionContext
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
		interpreter.visitSqlSelection( this );
	}
}
