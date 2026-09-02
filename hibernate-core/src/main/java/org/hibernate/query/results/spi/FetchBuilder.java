/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

import org.hibernate.Incubating;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.function.BiConsumer;

/**
 * Responsible for building a single {@link Fetch} instance.
 * Given the following HQL for illustration,
 * <pre>
 *     select b from Book b join fetch b.authors
 * </pre>
 * we have a single fetch : `Book(b).authors`
 *
 * @see ResultBuilder
 * @see ResultBuilder#visitFetchBuilders(BiConsumer)
 * @see FetchBuilder#visitFetchBuilders(BiConsumer)
 *
 * @author Steve Ebersole
 */
@Incubating
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface FetchBuilder extends GraphNodeBuilder {
	/// Builds and supplies one fetch node.
	///
	/// @see Fetch
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState);

	/// Supplies nested fetch builders to the visitor.
	///
	/// @see FetchBuilder
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	default void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
	}

	FetchBuilder cacheKeyInstance();
}
