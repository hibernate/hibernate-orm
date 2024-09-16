/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.Incubating;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
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
	DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState);

	Class<?> getJavaType();

	ResultBuilder cacheKeyInstance();

	default void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
	}
}
