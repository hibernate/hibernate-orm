/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

import org.hibernate.sql.results.graph.Fetchable;

/// Builds a fetch defined by legacy `hbm.xml` mappings or by calls to
/// [org.hibernate.query.NativeQuery#addFetch] and related methods.
///
/// Supply an implementation through
/// [ResultSetMapping#addLegacyFetchBuilder(LegacyFetchBuilder)].
///
/// @see ResultSetMapping#addLegacyFetchBuilder(LegacyFetchBuilder)
///
/// @author Steve Ebersole
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface LegacyFetchBuilder extends FetchBuilder {
	/**
	 * The table-alias associated with the fetch modeled by this builder.
	 */
	String getTableAlias();

	/**
	 * The alias for the node (result or fetch) which owns the fetch modeled by this builder.
	 */
	String getOwnerAlias();

	/**
	 * The name of the model-part being fetched.
	 */
	String getFetchableName();

	@Override
	LegacyFetchBuilder cacheKeyInstance();

	Fetchable getFetchable();
}
