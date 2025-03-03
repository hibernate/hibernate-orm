/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * ResultBuilder specialization for cases involving embeddable results.
 *
 * @author Steve Ebersole
 */
public interface ResultBuilderEmbeddable extends ResultBuilder {
	@Override
	EmbeddableResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState);
}
