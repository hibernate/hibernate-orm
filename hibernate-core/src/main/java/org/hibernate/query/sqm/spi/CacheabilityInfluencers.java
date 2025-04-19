/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmStatement;

import java.util.function.BooleanSupplier;

// Used by Hibernate Reactive
public interface CacheabilityInfluencers {
	boolean isQueryPlanCacheable();

	String getQueryString();

	Object getQueryStringCacheKey();

	SqmStatement<?> getSqmStatement();

	QueryOptions getQueryOptions();

	LoadQueryInfluencers getLoadQueryInfluencers();

	BooleanSupplier hasMultiValuedParameterBindingsChecker();
}
