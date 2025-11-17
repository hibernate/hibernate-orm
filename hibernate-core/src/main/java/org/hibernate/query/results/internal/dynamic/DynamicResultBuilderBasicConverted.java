/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import jakarta.persistence.AttributeConverter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.converter.internal.AttributeConverterBean;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Objects;

/**
 * A ResultBuilder for explicitly converted scalar results
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderBasicConverted<O,R> implements DynamicResultBuilderBasic {
	private final String columnAlias;
	private final BasicValueConverter<O,R> basicValueConverter;

	public DynamicResultBuilderBasicConverted(
			String columnAlias,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			AttributeConverter<O, R> converter,
			SessionFactoryImplementor sessionFactory) {
		this.columnAlias = columnAlias;
		final var javaTypeRegistry = sessionFactory.getTypeConfiguration().getJavaTypeRegistry();
		@SuppressWarnings("unchecked")
		final var converterClass = (Class<AttributeConverter<O, R>>) converter.getClass();
		this.basicValueConverter = new AttributeConverterBean<>(
				new ProvidedInstanceManagedBeanImpl<>( converter ),
				javaTypeRegistry.resolveDescriptor( converterClass ),
				javaTypeRegistry.resolveDescriptor( domainJavaType ),
				javaTypeRegistry.resolveDescriptor( jdbcJavaType )
		);
	}

	public DynamicResultBuilderBasicConverted(
			String columnAlias,
			Class<O> domainJavaType,
			Class<R> jdbcJavaType,
			Class<? extends AttributeConverter<O, R>> converterJavaType,
			SessionFactoryImplementor sessionFactory) {
		this.columnAlias = columnAlias;
		final var beans = sessionFactory.getManagedBeanRegistry();
		final var javaTypeRegistry = sessionFactory.getTypeConfiguration().getJavaTypeRegistry();
		this.basicValueConverter = new AttributeConverterBean<>(
				beans.getBean( converterJavaType ),
				javaTypeRegistry.resolveDescriptor( converterJavaType ),
				javaTypeRegistry.resolveDescriptor( domainJavaType ),
				javaTypeRegistry.resolveDescriptor( jdbcJavaType )
		);
	}

	@Override
	public Class<?> getJavaType() {
		return basicValueConverter.getDomainJavaType().getJavaTypeClass();
	}

	@Override
	public DynamicResultBuilderBasicConverted<?,?> cacheKeyInstance() {
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

		final String columnName =
				columnAlias != null
						? columnAlias
						: jdbcResultsMetadata.resolveColumnName( resultPosition + 1 );
		final var sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnName ),
						state -> resultSetMappingSqlSelection( jdbcResultsMetadata, resultPosition, typeConfiguration )
				),
				basicValueConverter.getRelationalJavaType(),
				null,
				typeConfiguration
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnAlias,
				basicValueConverter.getDomainJavaType(),
				basicValueConverter,
				null,
				false,
				false
		);
	}

	private ResultSetMappingSqlSelection resultSetMappingSqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata, int resultPosition, TypeConfiguration typeConfiguration) {
		final int jdbcPosition =
				columnAlias != null
						? jdbcResultsMetadata.resolveColumnPosition( columnAlias )
						: resultPosition + 1;
		final var basicType = jdbcResultsMetadata.resolveType(
				jdbcPosition,
				basicValueConverter.getRelationalJavaType(),
				typeConfiguration
		);
		final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );
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

		final var that = (DynamicResultBuilderBasicConverted<?, ?>) o;

		return Objects.equals( columnAlias, that.columnAlias )
			&& basicValueConverter.equals( that.basicValueConverter );
	}

	@Override
	public int hashCode() {
		int result = columnAlias != null ? columnAlias.hashCode() : 0;
		result = 31 * result + basicValueConverter.hashCode();
		return result;
	}
}
