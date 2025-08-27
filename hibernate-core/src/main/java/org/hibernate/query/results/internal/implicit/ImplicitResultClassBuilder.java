/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import jakarta.persistence.NamedNativeQuery;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * ResultBuilder for handling {@link NamedNativeQuery#resultClass()} when the
 * class does not refer to an entity
 *
 * @author Steve Ebersole
 */
public class ImplicitResultClassBuilder implements ResultBuilder {
	private final Class<?> suppliedResultClass;

	public ImplicitResultClassBuilder(Class<?> suppliedResultClass) {
		this.suppliedResultClass = suppliedResultClass;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		assert resultPosition == 0;

		final SqlAstCreationState sqlAstCreationState = domainResultCreationState.getSqlAstCreationState();
		final TypeConfiguration typeConfiguration = sqlAstCreationState.getCreationContext().getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final int jdbcResultPosition = 1;

		final String columnName = jdbcResultsMetadata.resolveColumnName( jdbcResultPosition );
		final BasicType<?> basicType = jdbcResultsMetadata.resolveType(
				jdbcResultPosition,
				typeConfiguration.getJavaTypeRegistry().resolveDescriptor( suppliedResultClass ),
				typeConfiguration
		);

		final SqlSelection selection =
				sqlSelection( resultPosition, sqlExpressionResolver, columnName, basicType, typeConfiguration );
		return new BasicResult<>( selection.getValuesArrayPosition(), columnName, basicType );
	}

	private static SqlSelection sqlSelection(
			int resultPosition,
			SqlExpressionResolver sqlExpressionResolver,
			String columnName,
			BasicType<?> basicType,
			TypeConfiguration typeConfiguration) {
		return sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnName ),
						state ->
								new ResultSetMappingSqlSelection( resultPosition, (BasicValuedMapping) basicType )
				),
				basicType.getMappedJavaType(),
				null,
				typeConfiguration
		);
	}

	@Override
	public Class<?> getJavaType() {
		return suppliedResultClass;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}
}
