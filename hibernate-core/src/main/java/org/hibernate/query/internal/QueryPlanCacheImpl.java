/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryPlanCache;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * Standard QueryInterpretations implementation
 *
 * @author Steve Ebersole
 */
public class QueryPlanCacheImpl implements QueryPlanCache {
	private final SessionFactoryImplementor sessionFactory;
	/**
	 * The default strong reference count.
	 */
	public static final int DEFAULT_PARAMETER_METADATA_MAX_COUNT = 128;
	/**
	 * The default soft reference count.
	 */
	public static final int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	/**
	 * the cache of the actual plans...
	 */
	private final BoundedConcurrentHashMap queryPlanCache;
	private final BoundedConcurrentHashMap sqmStatementCache;

	public QueryPlanCacheImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		Integer maxQueryPlanCount = ConfigurationHelper.getInteger(
				Environment.QUERY_PLAN_CACHE_MAX_SIZE,
				sessionFactory.getProperties()
		);
		if ( maxQueryPlanCount == null ) {
			maxQueryPlanCount = ConfigurationHelper.getInt(
					Environment.QUERY_PLAN_CACHE_MAX_SIZE,
					sessionFactory.getProperties(),
					DEFAULT_QUERY_PLAN_MAX_COUNT
			);
		}

		queryPlanCache = new BoundedConcurrentHashMap( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
		sqmStatementCache = new BoundedConcurrentHashMap( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
	}

	@Override
	public SelectQueryPlan getSelectQueryPlan(Key key) {
		// todo (6.0) : Log and stats, see HHH-12855
		return (SelectQueryPlan) queryPlanCache.get( key );
	}

	@Override
	public void cacheSelectQueryPlan(Key key, SelectQueryPlan plan) {
		// todo (6.0) : LOG, see HHH-12855
		queryPlanCache.putIfAbsent( key, plan );
	}

	@Override
	public NonSelectQueryPlan getNonSelectQueryPlan(Key key) {
		// todo (6.0) : implement
		return null;
	}

	@Override
	public void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan) {
		// todo (6.0) : implement
	}

	@Override
	public SqmStatement getSqmStatement(String queryString) {
		return (SqmStatement) sqmStatementCache.get( queryString );
	}

	@Override
	public void cacheSqmStatement(String key, SqmStatement sqmStatement) {
		// todo (6.0) : Log and stats, see HHH-12855
		sqmStatementCache.putIfAbsent( key, sqmStatement );
	}

	@Override
	public void close() {
		// todo (6.0) : clear maps/caches and LOG
		queryPlanCache.clear();
	}
}
