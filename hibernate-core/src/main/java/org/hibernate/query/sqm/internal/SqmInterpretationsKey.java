/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;

import static java.lang.Boolean.TRUE;
import static org.hibernate.query.spi.AbstractSelectionQuery.CRITERIA_HQL_STRING;

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
		if ( isCacheable (keySource ) ) {
			return new SqmInterpretationsKey(
					keySource.getQueryString(),
					keySource.getResultType(),
					keySource.getQueryOptions().getLockOptions(),
					keySource.getQueryOptions().getTupleTransformer(),
					keySource.getQueryOptions().getResultListTransformer(),
					new HashSet<>( keySource.getLoadQueryInfluencers().getEnabledFetchProfileNames() )
			);
		}
		else {
			return null;
		}
	}

	private static boolean isCacheable(InterpretationsKeySource keySource) {
		assert keySource.getQueryOptions().getAppliedGraph() != null;

		// for now at least, skip caching Criteria-based plans
		// - especially wrt parameters atm; this works with HQL because the
		// parameters are part of the query string; with Criteria, they're not.
		return ! CRITERIA_HQL_STRING.equals( keySource.getQueryString() )
				// At the moment we cannot cache query plan if there is filter enabled.
			&& ! keySource.getLoadQueryInfluencers().hasEnabledFilters()
				// At the moment we cannot cache query plan if it has an entity graph
			&& keySource.getQueryOptions().getAppliedGraph().getSemantic() == null
				// todo (6.0) : this one may be ok because of how I implemented multi-valued param handling
				// - the expansion is done per-execution based on the "static" SQM
				// - Note from Christian: The call to domainParameterXref.clearExpansions()
				//   in ConcreteSqmSelectQueryPlan is a concurrency issue when cached
				// - This could be solved by using a method-local clone of domainParameterXref
				//   when multi-valued params exist
			&& ! keySource.hasMultiValuedParameterBindingsChecker().get() == TRUE;
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
	private final TupleTransformer<?> tupleTransformer;
	private final ResultListTransformer<?> resultListTransformer;
	private final Collection<String> enabledFetchProfiles;

	private SqmInterpretationsKey(
			String query,
			Class<?> resultType,
			LockOptions lockOptions,
			TupleTransformer<?> tupleTransformer,
			ResultListTransformer<?> resultListTransformer,
			Collection<String> enabledFetchProfiles) {
		this.query = query;
		this.resultType = resultType;
		this.lockOptions = lockOptions;
		this.tupleTransformer = tupleTransformer;
		this.resultListTransformer = resultListTransformer;
		this.enabledFetchProfiles = enabledFetchProfiles;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new SqmInterpretationsKey(
				query,
				resultType,
				// Since lock options are mutable, we need a copy for the cache key
				lockOptions.makeCopy(),
				tupleTransformer,
				resultListTransformer,
				enabledFetchProfiles
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
				&& areEqual( lockOptions, that.lockOptions )
				&& areEqual( tupleTransformer, that.tupleTransformer )
				&& areEqual( resultListTransformer, that.resultListTransformer )
				&& areEqual( enabledFetchProfiles, that.enabledFetchProfiles );
	}

	private <T> boolean areEqual(T o1, T o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	@Override
	public int hashCode() {
		return query.hashCode();
	}
}
