/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import java.util.function.BiFunction;

import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * ResultBuilder specialization for cases involving scalar results.
 *
 * @see jakarta.persistence.ColumnResult
 *
 * @author Steve Ebersole
 */
public interface ResultBuilderBasicValued extends ResultBuilder {
	@Override
	BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState);
}
