/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;

import static java.lang.Boolean.TRUE;

/**
 * @author Steve Ebersole
 */
public class SqmInterpretationsKey implements QueryInterpretationCache.Key {
	public interface CacheabilityInfluencers {
		String getQueryString();
		QueryOptions getQueryOptions();
		LoadQueryInfluencers getLoadQueryInfluencers();
		Supplier<Boolean> hasMultiValuedParameterBindingsChecker();
	}

	public interface InterpretationsKeySource extends CacheabilityInfluencers {
		Class<?> getResultType();
	}

	public static SqmInterpretationsKey createInterpretationsKey(InterpretationsKeySource keySource) {
		if ( ! isCacheable( keySource ) ) {
			return null;
		}

		return new SqmInterpretationsKey(
				keySource.getQueryString(),
				keySource.getResultType(),
				keySource.getQueryOptions().getLockOptions()
		);
	}
	@SuppressWarnings("RedundantIfStatement")
	private static boolean isCacheable(InterpretationsKeySource keySource) {
		assert keySource.getQueryOptions().getAppliedGraph() != null;

		if ( QuerySqmImpl.CRITERIA_HQL_STRING.equals( keySource.getQueryString() ) ) {
			// for now at least, skip caching Criteria-based plans
			//		- especially wrt parameters atm; this works with HQL because the parameters
			//			are part of the query string; with Criteria, they are not.
			return false;
		}

		if ( keySource.getLoadQueryInfluencers().hasEnabledFilters() ) {
			// At the moment we cannot cache query plan if there is filter enabled.
			return false;
		}

		if ( keySource.getQueryOptions().getAppliedGraph().getSemantic() != null ) {
			// At the moment we cannot cache query plan if there is an
			// EntityGraph enabled.
			return false;
		}

		if ( keySource.hasMultiValuedParameterBindingsChecker().get() == TRUE ) {
			// todo (6.0) : this one may be ok because of how I implemented multi-valued param handling
			//		- the expansion is done per-execution based on the "static" SQM
			//  - Note from Christian: The call to domainParameterXref.clearExpansions() in ConcreteSqmSelectQueryPlan is a concurrency issue when cached
			//  - This could be solved by using a method-local clone of domainParameterXref when multi-valued params exist
			return false;
		}

		return true;
	}

	public static QueryInterpretationCache.Key generateNonSelectKey(InterpretationsKeySource keyDetails) {
		// todo (6.0) : do we want to cache non-select plans?  If so, what requirements?
		//		- very minimum is that it be a "simple" (non-multi-table) statement
		//
		// for now... no caching of non-select plans
		return null;
	}


	private final String query;
	private final Class<?> resultType;
	private final LockOptions lockOptions;

	private SqmInterpretationsKey(
			String query,
			Class<?> resultType,
			LockOptions lockOptions) {
		this.query = query;
		this.resultType = resultType;
		this.lockOptions = lockOptions;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new SqmInterpretationsKey(
				query,
				resultType,
				// Since lock options are mutable, we need a copy for the cache key
				lockOptions.makeCopy()
		);
	}

	@Override
	public String getQueryString() {
		return query;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final SqmInterpretationsKey that = (SqmInterpretationsKey) o;
		return query.equals( that.query )
				&& areEqual( resultType, that.resultType )
				&& areEqual( lockOptions, that.lockOptions );
	}

	private <T> boolean areEqual(T o1, T o2) {
		if ( o1 == null ) {
			return o2 == null;
		}
		else {
			return o1.equals( o2 );
		}
	}

	@Override
	public int hashCode() {
		return query.hashCode();
	}
}
