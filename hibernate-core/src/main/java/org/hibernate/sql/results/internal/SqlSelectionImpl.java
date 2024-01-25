/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlExpressionAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;

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
	private final JavaType<?> jdbcJavaType;
	private final boolean virtual;
	private transient ValueExtractor valueExtractor;

	public SqlSelectionImpl(Expression sqlExpression) {
		this( 0, -1, null, sqlExpression, false );
	}

	public SqlSelectionImpl(int valuesArrayPosition, Expression sqlExpression) {
		this( valuesArrayPosition + 1, valuesArrayPosition, null, sqlExpression, false );
	}

	public SqlSelectionImpl(int jdbcPosition, int valuesArrayPosition, Expression sqlExpression, boolean virtual) {
		this( jdbcPosition, valuesArrayPosition, null, sqlExpression, virtual );
	}

	public SqlSelectionImpl(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaType<?> jdbcJavaType,
			Expression sqlExpression,
			boolean virtual) {
		this(
				jdbcPosition,
				valuesArrayPosition,
				sqlExpression,
				jdbcJavaType,
				virtual,
				null
		);
	}

	protected SqlSelectionImpl(
			int jdbcPosition,
			int valuesArrayPosition,
			Expression sqlExpression,
			JavaType<?> jdbcJavaType,
			boolean virtual,
			ValueExtractor valueExtractor) {
		this.jdbcPosition = jdbcPosition;
		this.valuesArrayPosition = valuesArrayPosition;
		this.sqlExpression = sqlExpression;
		this.jdbcJavaType = jdbcJavaType;
		this.virtual = virtual;
		this.valueExtractor = valueExtractor;
	}

	private static ValueExtractor determineValueExtractor(Expression sqlExpression, JavaType<?> jdbcJavaType) {
		final JdbcMappingContainer expressionType = sqlExpression.getExpressionType();
		final JdbcMapping jdbcMapping = expressionType == null
				? JavaObjectType.INSTANCE
				: expressionType.getSingleJdbcMapping();
		if ( jdbcJavaType == null || jdbcMapping.getMappedJavaType() == jdbcJavaType ) {
			return jdbcMapping.getJdbcValueExtractor();
		}
		else {
			return jdbcMapping.getJdbcType().getExtractor( jdbcJavaType );
		}
	}


	@Override
	public Expression getExpression() {
		return sqlExpression;
	}

	@Override
	public ValueExtractor getJdbcValueExtractor() {
		ValueExtractor extractor = valueExtractor;
		if ( extractor == null ) {
			valueExtractor = extractor = determineValueExtractor( sqlExpression, jdbcJavaType );
		}
		return extractor;
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
	public boolean isVirtual() {
		return virtual;
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
				Objects.equals( sqlExpression, that.getExpression() ) &&
				virtual == that.isVirtual();
	}

	@Override
	public int hashCode() {
		return Objects.hash( jdbcPosition, valuesArrayPosition, sqlExpression, virtual );
	}
}
