/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class ScalarDomainResultBuilder<T> implements ResultBuilder {
	private final JavaType<T> typeDescriptor;

	public ScalarDomainResultBuilder(JavaType<T> typeDescriptor) {
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
		final SqlExpressionResolver sqlExpressionResolver =
				domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		final TypeConfiguration typeConfiguration =
				domainResultCreationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								Integer.toString( resultPosition + 1 )
						),
						processingState -> {
							final BasicType<?> basicType = jdbcResultsMetadata.resolveType(
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
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ScalarDomainResultBuilder<?> that = (ScalarDomainResultBuilder<?>) o;

		return typeDescriptor.equals( that.typeDescriptor );
	}

	@Override
	public int hashCode() {
		return typeDescriptor.hashCode();
	}
}
