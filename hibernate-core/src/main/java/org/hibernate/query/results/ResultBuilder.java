/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.Incubating;
import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * Responsible for building a single {@link DomainResult} instance as part of
 * the overall mapping of native / procedure query results.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ResultBuilder {
	/**
	 * Build a result
	 *
	 * @param jdbcResultsMetadata The JDBC values and metadata
	 * @param resultPosition The position in the domain results for the result to be built
	 * @param legacyFetchResolver Support for allowing some legacy-style fetch resolution
	 * @param domainResultCreationState Access to useful stuff
	 */
	DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState);

	/**
	 * The Java type of the value returned for a {@linkplain DomainResult result} built by this builder.
	 *
	 * @see DomainResult#getResultJavaType()
	 */
	Class<?> getJavaType();

	ResultBuilder cacheKeyInstance();

	default void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
	}
}
