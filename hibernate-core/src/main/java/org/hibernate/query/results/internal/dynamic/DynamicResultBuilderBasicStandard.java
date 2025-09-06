/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Objects;

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
			DomainResultCreationState domainResultCreationState) {
		final var sqlAstCreationState = domainResultCreationState.getSqlAstCreationState();
		final var typeConfiguration = sqlAstCreationState.getCreationContext().getTypeConfiguration();
		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final var expression = sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( columnName ),
				state -> resultSetMappingSqlSelection( jdbcResultsMetadata, typeConfiguration )
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
			final var jdbcMapping = expression.getExpressionType().getSingleJdbcMapping();
			javaType = jdbcMapping.getMappedJavaType();
			jdbcJavaType = jdbcMapping.getJdbcJavaType();
			converter = jdbcMapping.getValueConverter();
		}
		final var sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				expression,
				jdbcJavaType,
				null,
				typeConfiguration
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

	private ResultSetMappingSqlSelection resultSetMappingSqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata, TypeConfiguration typeConfiguration) {
		final int jdbcPosition =
				columnPosition > 0
						? columnPosition
						: jdbcResultsMetadata.resolveColumnPosition( columnName );
		final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
		final var basicType =
				explicitType != null
						? explicitType
						: jdbcResultsMetadata.resolveType( jdbcPosition, explicitJavaType, typeConfiguration );
		return new ResultSetMappingSqlSelection( valuesArrayPosition, (BasicValuedMapping) basicType );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final var that = (DynamicResultBuilderBasicStandard) o;
		return columnPosition == that.columnPosition
			&& columnName.equals( that.columnName )
			&& resultAlias.equals( that.resultAlias )
			&& Objects.equals( explicitType, that.explicitType )
			&& Objects.equals( explicitJavaType, that.explicitJavaType );
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
