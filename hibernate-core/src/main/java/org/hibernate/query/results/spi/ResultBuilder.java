/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

import org.hibernate.Incubating;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.function.BiConsumer;

/// Builds one [DomainResult] for a native-query result-set mapping.
///
/// Given `select b from Book b join fetch b.authors`, the result builder
/// produces the single `Book(b)` result. Implement this contract when a
/// provider needs a custom result shape, and register the instance through
/// [ResultSetMapping#addResultBuilder(ResultBuilder)].
///
/// @see FetchBuilder
/// @see ResultSetMapping#addResultBuilder(ResultBuilder)
///
/// @author Steve Ebersole
@Incubating
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface ResultBuilder extends GraphNodeBuilder {
	/// Builds and supplies one domain result.
	///
	/// @see FetchBuilder
	/// @see DomainResult
	///
	/// @param jdbcResultsMetadata the JDBC values and metadata
	/// @param resultPosition the position in the domain results for the result to be built
	/// @param domainResultCreationState access to result-graph creation services
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState);

	/**
	 * The Java type of the value returned for a {@linkplain DomainResult result} built by this builder.
	 *
	 * @see DomainResult#getResultJavaType()
	 */
	Class<?> getJavaType();

	ResultBuilder cacheKeyInstance();

	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	default void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
	}
}
