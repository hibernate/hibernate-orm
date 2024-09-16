/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultSetMappingSqlSelection;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.NamedNativeQuery;

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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		assert resultPosition == 0;

		final SessionFactoryImplementor sessionFactory =
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
		final SqlExpressionResolver sqlExpressionResolver =
				domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();

		final int jdbcResultPosition = 1;

		final String columnName = jdbcResultsMetadata.resolveColumnName( jdbcResultPosition );
		final BasicType<?> basicType = jdbcResultsMetadata.resolveType(
				jdbcResultPosition,
				typeConfiguration.getJavaTypeRegistry().resolveDescriptor( suppliedResultClass ),
				typeConfiguration
		);

		final SqlSelection selection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey( columnName ),
						(state) -> new ResultSetMappingSqlSelection( resultPosition, (BasicValuedMapping) basicType )
				),
				basicType.getMappedJavaType(),
				null,
				typeConfiguration
		);

		return new BasicResult<>( selection.getValuesArrayPosition(), columnName, basicType );
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
