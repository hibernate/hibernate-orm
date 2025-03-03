/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.Locale;

import static org.hibernate.query.results.internal.ResultsHelper.impl;
import static org.hibernate.query.results.internal.ResultsHelper.jdbcPositionToValuesArrayPosition;

/**
 * DynamicResultBuilder based on a named mapped attribute
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderAttribute implements DynamicResultBuilder, NativeQuery.ReturnProperty {
	private final BasicAttributeMapping attributeMapping;
	private final String columnAlias;
	private final String entityName;
	private final String attributePath;

	public DynamicResultBuilderAttribute(
			SingularAttributeMapping attributeMapping,
			String columnAlias,
			String entityName,
			String attributePath) {
		final boolean allowable = attributeMapping instanceof BasicAttributeMapping;
		if ( !allowable ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Specified attribute [%s.%s] must be basic: %s",
							entityName,
							attributePath,
							attributeMapping
					)
			);
		}

		this.attributeMapping = (BasicAttributeMapping) attributeMapping;
		this.columnAlias = columnAlias;
		this.entityName = entityName;
		this.attributePath = attributePath;
	}

	@Override
	public Class<?> getJavaType() {
		return attributeMapping.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DynamicResultBuilderAttribute cacheKeyInstance() {
		return this;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final SqlSelection sqlSelection =
				sqlSelection( jdbcResultsMetadata, domainResultCreationState, impl( domainResultCreationState ) );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				columnAlias,
				attributeMapping.getJdbcMapping(),
				null,
				false,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection sqlSelection(
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState,
			DomainResultCreationStateImpl domainResultCreationStateImpl) {
		return domainResultCreationStateImpl.resolveSqlSelection(
				domainResultCreationStateImpl.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnAlias ),
						processingState -> {
							final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
							final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );
							return new ResultSetMappingSqlSelection( valuesArrayPosition, attributeMapping );
						}
				),
				attributeMapping.getJdbcMapping().getJdbcJavaType(),
				null,
				domainResultCreationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
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

		final DynamicResultBuilderAttribute that = (DynamicResultBuilderAttribute) o;
		return attributeMapping.equals( that.attributeMapping )
				&& columnAlias.equals( that.columnAlias )
				&& entityName.equals( that.entityName )
				&& attributePath.equals( that.attributePath );
	}

	@Override
	public int hashCode() {
		int result = attributeMapping.hashCode();
		result = 31 * result + columnAlias.hashCode();
		result = 31 * result + entityName.hashCode();
		result = 31 * result + attributePath.hashCode();
		return result;
	}
}
