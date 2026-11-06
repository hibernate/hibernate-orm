package org.hibernate.query.sqm.internal;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.FilterImpl;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.sql.exec.internal.FilterJdbcParameter;


/**
 * @author Steve Ebersole
 */
public final class SqmInterpretationsKey implements QueryInterpretationCache.Key {

	public static SqmInterpretationsKey createInterpretationsKey(InterpretationsKeySource keySource) {
		return createInterpretationsKey( keySource, keySource.getQueryOptions().getLockOptions() );
	}

	public static SqmInterpretationsKey createInterpretationsKey(InterpretationsKeySource keySource, LockOptions lockOptions) {
		if ( isCacheable ( keySource ) ) {
			final Object query = keySource.getQueryStringCacheKey();
			final var influencers = keySource.getLoadQueryInfluencers();
			final List<Object> enabledFilters = enabledFilters( influencers );
			final int queryHashCode = query instanceof SqmStatement<?> statement ? statement.cacheHashCode() : query.hashCode();
			return new SqmInterpretationsKey(
					query,
					keySource.unnamedParameterIndices(),
					enabledFilters == null ? queryHashCode : 31 * queryHashCode + enabledFilters.hashCode(),
					keySource.getResultType(),
					lockOptions,
					memoryEfficientDefensiveSetCopy( influencers.getEnabledFetchProfileNames() ),
					enabledFilters,
					influencers.getTemporalIdentifier() != null
			);
		}
		else {
			return null;
		}
	}

	/**
	 * The names of the enabled filters, in order, each followed by the element
	 * count of every multivalued argument. Filter arguments are bound for each
	 * execution, but the number of JDBC parameters depends on the element count.
	 * The argument values themselves are never part of the key.
	 */
	private static List<Object> enabledFilters(LoadQueryInfluencers influencers) {
		if ( !influencers.hasEnabledFilters() ) {
			return null;
		}
		final var filters = influencers.getEnabledFilters();
		final List<Object> key = new ArrayList<>( filters.size() );
		for ( var filter : filters.values() ) {
			key.add( filter.getName() );
			final var definition = ( (FilterImpl) filter ).getFilterDefinition();
			for ( String parameterName : definition.getParameterNames() ) {
				final Object value = filter.getParameterValue( parameterName );
				if ( FilterJdbcParameter.isMultiValued( value, definition.getParameterJdbcMapping( parameterName ) ) ) {
					key.add( new MultiValuedFilterParameter( parameterName, FilterJdbcParameter.elementCount( value ) ) );
				}
			}
		}
		return key;
	}

	private record MultiValuedFilterParameter(String name, int elementCount) {
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
			&& keySource.getQueryOptions().isLimitInMemoryEnabled() != Boolean.TRUE
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
	@Nullable
	private final int[] unnamedParameterIndices;
	private final Class<?> resultType;
	private final LockOptions lockOptions;
	private final Collection<String> enabledFetchProfiles;
	@Nullable
	private final List<Object> enabledFilters;
	private final boolean historical;
	private final int hashCode;

	private SqmInterpretationsKey(
			Object query,
			@Nullable int[] unnamedParameterIndices,
			int hashCode,
			Class<?> resultType,
			LockOptions lockOptions,
			Collection<String> enabledFetchProfiles,
			@Nullable List<Object> enabledFilters,
			boolean historical) {
		this.historical = historical;
		assert query.getClass() == String.class || query instanceof SqmStatement<?>;
		this.query = query;
		this.unnamedParameterIndices = unnamedParameterIndices;
		this.hashCode = hashCode;
		this.resultType = resultType;
		this.lockOptions = lockOptions;
		this.enabledFetchProfiles = enabledFetchProfiles;
		this.enabledFilters = enabledFilters;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new SqmInterpretationsKey(
				query,
				unnamedParameterIndices,
				hashCode,
				resultType,
				// Since lock options might be mutable, we need a copy for the cache key
				lockOptions.makeDefensiveCopy(),
				enabledFetchProfiles,
				enabledFilters,
				historical
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
			&& ( query.getClass() == String.class
				? query.equals( that.query )
				: ((SqmStatement<?>) query).isCompatible( that.query ) )
			&& Arrays.equals( this.unnamedParameterIndices, that.unnamedParameterIndices )
			&& Objects.equals( this.resultType, that.resultType )
			&& Objects.equals( this.lockOptions, that.lockOptions )
			&& Objects.equals( this.enabledFetchProfiles, that.enabledFetchProfiles )
			&& Objects.equals( this.enabledFilters, that.enabledFilters )
			&& this.historical == that.historical;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return query.toString() + (resultType != null ? " : " + resultType.getSimpleName() : "");
	}
}
