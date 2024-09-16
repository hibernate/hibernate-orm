/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.Incubating;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * Responsible for building a single {@link DomainResult} instance as part of
 * the overall mapping of native / procedure query results.
 *
 * @apiNote By definition a fetch is a reference to the fetched ModelPart
 * @author Steve Ebersole
 */
@Incubating
public interface FetchBuilder {
	Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState);

	default void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
	}

	FetchBuilder cacheKeyInstance();
}
