/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/// Builds a fetch for a basic-valued mapping.
///
/// Supply an implementation while visiting the fetch builders of a containing
/// [ResultBuilder] or [FetchBuilder].
///
/// @see ResultBuilder#visitFetchBuilders(java.util.function.BiConsumer)
/// @see FetchBuilder#visitFetchBuilders(java.util.function.BiConsumer)
///
/// @author Steve Ebersole
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface FetchBuilderBasicValued extends FetchBuilder {
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState);
}
