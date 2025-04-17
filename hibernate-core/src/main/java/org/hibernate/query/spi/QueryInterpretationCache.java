/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.sql.spi.ParameterInterpretation;

/**
 * Cache for various parts of translating or interpreting queries.
 *
 * @see org.hibernate.cfg.AvailableSettings#QUERY_PLAN_CACHE_ENABLED
 * @see org.hibernate.cfg.AvailableSettings#QUERY_PLAN_CACHE_MAX_SIZE
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryInterpretationCache {
	interface Key {
		/**
		 * The possibility for a cache key to do defensive copying in case it has mutable state.
		 */
		default Key prepareForStore() {
			return this;
		}
		String getQueryString();
	}

	int getNumberOfCachedHqlInterpretations();
	int getNumberOfCachedQueryPlans();

	<R> HqlInterpretation<R> resolveHqlInterpretation(String queryString, Class<R> expectedResultType, HqlTranslator translator);
	<R> void cacheHqlInterpretation(Object cacheKey, HqlInterpretation<R> hqlInterpretation);

	<R> SelectQueryPlan<R> resolveSelectQueryPlan(Key key, Supplier<SelectQueryPlan<R>> creator);

	NonSelectQueryPlan getNonSelectQueryPlan(Key key);
	void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan);

	ParameterInterpretation resolveNativeQueryParameters(String queryString, Function<String, ParameterInterpretation> creator);

	boolean isEnabled();

	/**
	 * Close the cache when the SessionFactory is closed.
	 * <p>
	 * Note that depending on the cache strategy implementation chosen, clearing the cache might not reclaim all the
	 * memory.
	 * <p>
	 * Typically, when using LIRS, clearing the cache only invalidates the entries but the outdated entries are kept in
	 * memory until they are replaced by others. It is not considered a memory leak as the cache is bounded.
	 */
	void close();

}
