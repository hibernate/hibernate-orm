/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmStatement;

import static java.lang.Boolean.TRUE;
import static org.hibernate.query.spi.AbstractSelectionQuery.CRITERIA_HQL_STRING;

/**
 * @author Steve Ebersole
 */
public final class SqmInterpretationsKey implements QueryInterpretationCache.Key {
	public interface CacheabilityInfluencers {
		boolean isQueryPlanCacheable();
		String getQueryString();
		SqmStatement<?> getSqmStatement();
		QueryOptions getQueryOptions();
		LoadQueryInfluencers getLoadQueryInfluencers();
		Supplier<Boolean> hasMultiValuedParameterBindingsChecker();
	}

	public interface InterpretationsKeySource extends CacheabilityInfluencers {
		Class<?> getResultType();
	}

	public static SqmInterpretationsKey createInterpretationsKey(InterpretationsKeySource keySource) {
		if ( isCacheable ( keySource ) ) {
			final Object query = CRITERIA_HQL_STRING.equals( keySource.getQueryString() )
					? keySource.getSqmStatement()
					: keySource.getQueryString();
			return new SqmInterpretationsKey(
					query,
					query.hashCode(),
					keySource.getResultType(),
					keySource.getQueryOptions().getLockOptions(),
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
			switch ( set.size() ) {
				case 0:
					return null;
				case 1:
					return Set.of( set.iterator().next() );
				case 2:
					final Iterator<String> iterator = set.iterator();
					return Set.of( iterator.next(), iterator.next() );
				default:
					return Set.copyOf( set );
			}
		}
	}

	private static boolean isCacheable(InterpretationsKeySource keySource) {
		assert keySource.getQueryOptions().getAppliedGraph() != null;

		// for now at least, skip caching Criteria-based plans
		// - especially wrt parameters atm; this works with HQL because the
		// parameters are part of the query string; with Criteria, they're not.
		return keySource.isQueryPlanCacheable()
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

	private final Object query;
	private final Class<?> resultType;
	private final LockOptions lockOptions;
	private final Collection<String> enabledFetchProfiles;
	private final int hashcode;

	private SqmInterpretationsKey(
			Object query,
			int hash,
			Class<?> resultType,
			LockOptions lockOptions,
			Collection<String> enabledFetchProfiles) {
		this.query = query;
		this.hashcode = hash;
		this.resultType = resultType;
		this.lockOptions = lockOptions;
		this.enabledFetchProfiles = enabledFetchProfiles;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new SqmInterpretationsKey(
				query,
				hashcode,
				resultType,
				// Since lock options might be mutable, we need a copy for the cache key
				lockOptions.makeDefensiveCopy(),
				enabledFetchProfiles
		);
	}

	@Override
	public String getQueryString() {
		return query instanceof String ? (String) query : null;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || SqmInterpretationsKey.class != o.getClass() ) {
			return false;
		}

		final SqmInterpretationsKey that = (SqmInterpretationsKey) o;
		return this.hashcode == o.hashCode() //check this first as some other checks are expensive
			&& query.equals( that.query )
			&& Objects.equals( resultType, that.resultType )
			&& Objects.equals( lockOptions, that.lockOptions )
			&& Objects.equals( enabledFetchProfiles, that.enabledFetchProfiles );
	}

	@Override
	public int hashCode() {
		return hashcode;
	}
}
