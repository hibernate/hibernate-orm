/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								Integer.toString( resultPosition + 1 )
						),
						processingState -> {
							final BasicType<?> basicType = jdbcResultsMetadata.resolveType(
									resultPosition + 1,
									typeDescriptor,
									processingState.getSqlAstCreationState().getCreationContext().getSessionFactory()
							);
							return new ResultSetMappingSqlSelection( resultPosition, (BasicValuedMapping) basicType );
						}
				),
				typeDescriptor,
				null,
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
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
