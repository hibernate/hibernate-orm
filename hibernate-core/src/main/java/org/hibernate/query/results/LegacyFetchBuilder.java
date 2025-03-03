/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import org.hibernate.sql.results.graph.Fetchable;

/**
 * Specialized FetchBuilder implementations which handle building fetches defined via:<ul>
 *     <li>{@code hbm.xml} definitions</li>
 *     <li>calls to {@link org.hibernate.query.NativeQuery#addFetch}, and friends</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
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
