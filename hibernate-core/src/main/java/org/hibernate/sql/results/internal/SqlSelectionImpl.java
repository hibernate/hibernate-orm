/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.ValueExtractor;

/**
 * @asciidoc
 *
 * ```
 * @Entity
 * class MyEntity {
 *     ...
 *     @Column ( name = "the_column", ... )
 *     public String getTheColumn() { ... }
 *
 *     @Convert ( ... )
 *     @Column ( name = "the_column", ... )
 *     ConvertedType getTheConvertedColumn() { ... }
 *
 * }
 * ```
 *
 * @author Steve Ebersole
 */
public class SqlSelectionImpl implements SqlSelection, SqlExpressionAccess {
	private final int jdbcPosition;
	private final int valuesArrayPosition;
	private final Expression sqlExpression;

	public SqlSelectionImpl(int jdbcPosition, int valuesArrayPosition, Expression sqlExpression) {
		this.jdbcPosition = jdbcPosition;
		this.valuesArrayPosition = valuesArrayPosition;
		this.sqlExpression = sqlExpression;
	}

	@Override
	public Expression getExpression() {
		return sqlExpression;
	}

	@Override
	public ValueExtractor getJdbcValueExtractor() {
		return ( (SqlExpressible) sqlExpression.getExpressionType() ).getJdbcMapping().getJdbcValueExtractor();
	}

	@Override
	public int getJdbcResultSetIndex() {
		return jdbcPosition;
	}

	@Override
	public int getValuesArrayPosition() {
		return valuesArrayPosition;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return getExpression().getExpressionType();
	}

	@Override
	public Expression getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public void accept(SqlAstWalker interpreter) {
		sqlExpression.accept( interpreter );
	}

	@Override
	public SqlSelection resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		if ( sqlExpression.getExpressionType() instanceof JavaObjectType ) {
			final BasicType<Object> resolvedType = jdbcResultsMetadata.resolveType(
					jdbcPosition,
					null,
					sessionFactory
			);
			return new ResolvedSqlSelection(
					jdbcPosition,
					valuesArrayPosition,
					sqlExpression,
					resolvedType
			);
		}
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final SqlSelection that = (SqlSelection) o;
		return jdbcPosition == that.getJdbcResultSetIndex() &&
				valuesArrayPosition == that.getValuesArrayPosition() &&
				Objects.equals( sqlExpression, that.getExpression() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( jdbcPosition, valuesArrayPosition, sqlExpression );
	}
}
