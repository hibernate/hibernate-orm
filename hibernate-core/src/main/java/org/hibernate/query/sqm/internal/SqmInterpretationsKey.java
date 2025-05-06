/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.hibernate.LockOptions;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;


/**
 * @author Steve Ebersole
 */
public final class SqmInterpretationsKey implements QueryInterpretationCache.Key {

	public static SqmInterpretationsKey createInterpretationsKey(InterpretationsKeySource keySource) {
		if ( isCacheable ( keySource ) ) {
			final Object query = keySource.getQueryStringCacheKey();
			return new SqmInterpretationsKey(
					query,
					query.hashCode(),
					keySource.getResultType(),
					keySource.getQueryOptions().getLockOptions(),
					keySource.getQueryOptions().getTupleTransformer(),
					keySource.getQueryOptions().getResultListTransformer(),
					memoryEfficientDefensiveSetCopy( keySource.getLoadQueryInfluencers().getEnabledFetchProfileNames() )
			);
		}
		else {
			return null;
		}
	}

	private static Collection<String> memoryEfficientDefensiveSetCopy(final Set<String> set) {
		if ( set == null ) {
			return null;
		}
		else {
			return switch ( set.size() ) {
				case 0 -> null;
				case 1 -> Set.of( set.iterator().next() );
				case 2 -> {
					final var iterator = set.iterator();
					yield Set.of( iterator.next(), iterator.next() );
				}
				default -> Set.copyOf( set );
			};
		}
	}

	private static boolean isCacheable(InterpretationsKeySource keySource) {
		assert keySource.getQueryOptions().getAppliedGraph() != null;

		// for now at least, skip caching Criteria-based plans
		// - especially wrt parameters atm; this works with HQL because the
		// parameters are part of the query string; with Criteria, they're not.
		return keySource.isQueryPlanCacheable()
				// At the moment we cannot cache query plan if there is filter enabled.
			&& !keySource.getLoadQueryInfluencers().hasEnabledFilters()
				// At the moment we cannot cache query plan if it has an entity graph
			&& keySource.getQueryOptions().getAppliedGraph().getSemantic() == null
				// todo (6.0) : this one may be ok because of how I implemented multi-valued param handling
				// - the expansion is done per-execution based on the "static" SQM
				// - Note from Christian: The call to domainParameterXref.clearExpansions()
				//   in ConcreteSqmSelectQueryPlan is a concurrency issue when cached
				// - This could be solved by using a method-local clone of domainParameterXref
				//   when multi-valued params exist
			&& !keySource.hasMultiValuedParameterBindingsChecker().getAsBoolean();
	}

	public static QueryInterpretationCache.Key generateNonSelectKey(InterpretationsKeySource keyDetails) {
		// todo (6.0) : do we want to cache non-select plans?  If so, what requirements?
		//		- very minimum is that it be a "simple" (non-multi-table) statement
		//
		// for now... no caching of non-select plans
		return null;
	}

	private final Object query;
	private final Class<?> resultType;
	private final LockOptions lockOptions;
	private final TupleTransformer<?> tupleTransformer;
	private final ResultListTransformer<?> resultListTransformer;
	private final Collection<String> enabledFetchProfiles;
	private final int hashCode;

	private SqmInterpretationsKey(
			Object query,
			int hash,
			Class<?> resultType,
			LockOptions lockOptions,
			TupleTransformer<?> tupleTransformer,
			ResultListTransformer<?> resultListTransformer,
			Collection<String> enabledFetchProfiles) {
		this.query = query;
		this.hashCode = hash;
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
				hashCode,
				resultType,
				// Since lock options might be mutable, we need a copy for the cache key
				lockOptions.makeDefensiveCopy(),
				tupleTransformer,
				resultListTransformer,
				enabledFetchProfiles
		);
	}

	@Override
	public String getQueryString() {
		return query instanceof String ? (String) query : null;
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}
		if ( !(other instanceof SqmInterpretationsKey that)) {
			return false;
		}
		return this.hashCode == that.hashCode //check this first as some other checks are expensive
			&& this.query.equals( that.query )
			&& Objects.equals( this.resultType, that.resultType )
			&& Objects.equals( this.lockOptions, that.lockOptions )
			&& Objects.equals( this.tupleTransformer, that.tupleTransformer )
			&& Objects.equals( this.resultListTransformer, that.resultListTransformer )
			&& Objects.equals( this.enabledFetchProfiles, that.enabledFetchProfiles );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
