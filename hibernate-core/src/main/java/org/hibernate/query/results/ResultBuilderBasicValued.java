/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

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
			DomainResultCreationState domainResultCreationState);
}
