/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/// External result builder implemented only in terms of supported result SPI.
///
/// @author Steve Ebersole
/// @since 8.0
public final class ExampleResultBuilder implements ResultBuilder {
	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final var typeConfiguration = domainResultCreationState.getSqlAstCreationState()
				.getCreationContext()
				.getTypeConfiguration();
		final var jdbcMapping = jdbcResultsMetadata.resolveType( resultPosition + 1, null, typeConfiguration );
		return new BasicResult<>( resultPosition, "fixture_result", jdbcMapping );
	}

	@Override
	public Class<?> getJavaType() {
		return Object.class;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}
}
