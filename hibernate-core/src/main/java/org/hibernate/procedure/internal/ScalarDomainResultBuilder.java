/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
class ScalarDomainResultBuilder<T> implements ResultBuilder {
	private final JavaType<T> typeDescriptor;

	ScalarDomainResultBuilder(JavaType<T> typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
	}

	@Override
	public Class<?> getJavaType() {
		return typeDescriptor.getJavaTypeClass();
	}

	@Override
	public DomainResult<T> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final var sqlAstCreationState = domainResultCreationState.getSqlAstCreationState();
		final var sqlExpressionResolver =
				sqlAstCreationState.getSqlExpressionResolver();
		final var typeConfiguration =
				sqlAstCreationState.getCreationContext().getTypeConfiguration();
		final var sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						createColumnReferenceKey(
								Integer.toString( resultPosition + 1 )
						),
						processingState -> {
							final var basicType = jdbcResultsMetadata.resolveType(
									resultPosition + 1,
									typeDescriptor,
									typeConfiguration
							);
							return new ResultSetMappingSqlSelection( resultPosition, (BasicValuedMapping) basicType );
						}
				),
				typeDescriptor,
				null,
				typeConfiguration
		);
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				null,
				(BasicType<?>) sqlSelection.getExpressionType(),
				null,
				false,
				false
		);
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof ScalarDomainResultBuilder<?> that ) ) {
			return false;
		}
		else {
			return typeDescriptor.equals( that.typeDescriptor );
		}
	}

	@Override
	public int hashCode() {
		return typeDescriptor.hashCode();
	}
}
