/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.Objects;
import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.query.NativeQuery;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Standard DynamicResultBuilder for basic values.
 *
 * @see NativeQuery#addScalar
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderBasicStandard implements DynamicResultBuilderBasic {
	private final String columnName;
	private final int columnPosition;
	private final String resultAlias;

	private final BasicType<?> explicitType;
	private final JavaType<?> explicitJavaType;

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias) {
		assert columnName != null;
		this.columnName = columnName;
		this.columnPosition = 0;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		this.explicitType = null;
		this.explicitJavaType = null;
	}

	public DynamicResultBuilderBasicStandard(int columnPosition) {
		assert columnPosition > 0;
		this.columnName = "c" + columnPosition;
		this.columnPosition = columnPosition;
		this.resultAlias = columnName;

		this.explicitType = null;
		this.explicitJavaType = null;
	}

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias, JavaType<?> explicitJavaType) {
		assert columnName != null;
		this.columnName = columnName;
		this.columnPosition = 0;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		assert explicitJavaType != null;
		this.explicitJavaType = explicitJavaType;

		this.explicitType = null;
	}

	public DynamicResultBuilderBasicStandard(String columnName, String resultAlias, BasicType<?> explicitType) {
		assert columnName != null;
		this.columnName = columnName;
		this.columnPosition = 0;
		this.resultAlias = resultAlias != null ? resultAlias : columnName;

		assert explicitType != null;
		this.explicitType = explicitType;

		this.explicitJavaType = null;
	}

	public DynamicResultBuilderBasicStandard(int columnPosition, BasicType<?> explicitType) {
		assert columnPosition > 0;
		this.columnName = "c" + columnPosition;
		this.columnPosition = columnPosition;
		this.resultAlias = columnName;

		assert explicitType != null;
		this.explicitType = explicitType;

		this.explicitJavaType = null;
	}

	@Override
	public Class<?> getJavaType() {
		if ( explicitJavaType != null ) {
			return explicitJavaType.getJavaTypeClass();
		}
		if ( explicitType != null ) {
			return explicitType.getJavaType();
		}
		return null;
	}

	public String getColumnName() {
		return columnName;
	}

	@Override
	public DynamicResultBuilderBasicStandard cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final SessionFactoryImplementor sessionFactory = domainResultCreationState.getSqlAstCreationState()
				.getCreationContext()
				.getSessionFactory();

		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		final Expression expression = sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( columnName ),
				state -> {
					final int jdbcPosition;
					if ( columnPosition > 0 ) {
						jdbcPosition = columnPosition;
					}
					else {
						jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnName );
					}
					final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );

					final BasicType<?> basicType;

					if ( explicitType != null ) {
						basicType = explicitType;
					}
					else {
						basicType = jdbcResultsMetadata.resolveType(
								jdbcPosition,
								explicitJavaType,
								sessionFactory
						);
					}
					return new ResultSetMappingSqlSelection( valuesArrayPosition, (BasicValuedMapping) basicType );
				}
		);

		final JavaType<?> javaType;
		final JavaType<?> jdbcJavaType;
		final BasicValueConverter<?, ?> converter;

		if ( explicitJavaType != null ) {
			javaType = explicitJavaType;
			jdbcJavaType = explicitJavaType;
			converter = null;
		}
		else {
			final JdbcMapping jdbcMapping = expression.getExpressionType().getSingleJdbcMapping();
			javaType = jdbcMapping.getMappedJavaType();
			jdbcJavaType = jdbcMapping.getJdbcJavaType();
			converter = jdbcMapping.getValueConverter();
		}
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				expression,
				jdbcJavaType,
				null,
				sessionFactory.getTypeConfiguration()
		);

		// StandardRowReader expects there to be a JavaType as part of the ResultAssembler.
		assert javaType != null;

		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultAlias,
				javaType,
				converter,
				null,
				false,
				false
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		DynamicResultBuilderBasicStandard that = (DynamicResultBuilderBasicStandard) o;

		if ( columnPosition != that.columnPosition ) {
			return false;
		}
		if ( !columnName.equals( that.columnName ) ) {
			return false;
		}
		if ( !resultAlias.equals( that.resultAlias ) ) {
			return false;
		}
		if ( !Objects.equals( explicitType, that.explicitType ) ) {
			return false;
		}
		return Objects.equals( explicitJavaType, that.explicitJavaType );
	}

	@Override
	public int hashCode() {
		int result = columnName.hashCode();
		result = 31 * result + columnPosition;
		result = 31 * result + resultAlias.hashCode();
		result = 31 * result + ( explicitType != null ? explicitType.hashCode() : 0 );
		result = 31 * result + ( explicitJavaType != null ? explicitJavaType.hashCode() : 0 );
		return result;
	}
}
