/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/// Builds an entity-valued result for a result-set mapping.
///
/// Supply an implementation through
/// [ResultSetMapping#addResultBuilder(ResultBuilder)].
///
/// @see jakarta.persistence.EntityResult
/// @see ResultSetMapping#addResultBuilder(ResultBuilder)
///
/// @author Steve Ebersole
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface ResultBuilderEntityValued extends ResultBuilder {
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	EntityResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState);
}
