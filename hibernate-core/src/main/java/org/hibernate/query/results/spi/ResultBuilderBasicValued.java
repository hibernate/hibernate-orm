/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/// Builds a scalar result for a result-set mapping.
///
/// Supply an implementation through
/// [ResultSetMapping#addResultBuilder(ResultBuilder)].
///
/// @see jakarta.persistence.ColumnResult
/// @see ResultSetMapping#addResultBuilder(ResultBuilder)
///
/// @author Steve Ebersole
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface ResultBuilderBasicValued extends ResultBuilder {
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState);
}
