/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.Filter;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Acts as a cache for compiled query plans, as well as query-parameter metadata.
 *
 * @see Environment#QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE
 * @see Environment#QUERY_PLAN_CACHE_MAX_SIZE
 *
 * @author Steve Ebersole
 */
public class QueryPlanCache implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueryPlanCache.class );

	@FunctionalInterface
	public interface QueryPlanCreator {
		HQLQueryPlan createQueryPlan(String queryString, boolean shallow, Map<String, Filter> enabledFilters, SessionFactoryImplementor factory);
	}

	/**
	 * The default strong reference count.
	 */
	public static final int DEFAULT_PARAMETER_METADATA_MAX_COUNT = 128;
	/**
	 * The default soft reference count.
	 */
	public static final int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	private final SessionFactoryImplementor factory;
	private QueryPlanCreator queryPlanCreator;

	/**
	 * the cache of the actual plans...
	 */
	private final BoundedConcurrentHashMap queryPlanCache;

	/**
	 * simple cache of param metadata based on query string.  Ideally, the original "user-supplied query"
	 * string should be used to obtain this metadata (i.e., not the para-list-expanded query string) to avoid
	 * unnecessary cache entries.
	 * <p></p>
	 * Used solely for caching param metadata for native-sql queries, see {@link #getSQLParameterMetadata} for a
	 * discussion as to why...
	 */
	private final BoundedConcurrentHashMap<ParameterMetadataKey,ParameterMetadataImpl> parameterMetadataCache;


	private NativeQueryInterpreter nativeQueryInterpreter;

	/**
	 * Constructs the QueryPlanCache to be used by the given SessionFactory
	 *
	 * @param factory The SessionFactory
	 */
	@SuppressWarnings("deprecation")
	public QueryPlanCache(final SessionFactoryImplementor factory, QueryPlanCreator queryPlanCreator) {
		this.factory = factory;
		this.queryPlanCreator = queryPlanCreator;

		Integer maxParameterMetadataCount = ConfigurationHelper.getInteger(
				Environment.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE,
				factory.getProperties()
		);
		if ( maxParameterMetadataCount == null ) {
			maxParameterMetadataCount = ConfigurationHelper.getInt(
					Environment.QUERY_PLAN_CACHE_MAX_STRONG_REFERENCES,
					factory.getProperties(),
					DEFAULT_PARAMETER_METADATA_MAX_COUNT
			);
		}
		Integer maxQueryPlanCount = ConfigurationHelper.getInteger(
				Environment.QUERY_PLAN_CACHE_MAX_SIZE,
				factory.getProperties()
		);
		if ( maxQueryPlanCount == null ) {
			maxQueryPlanCount = ConfigurationHelper.getInt(
					Environment.QUERY_PLAN_CACHE_MAX_SOFT_REFERENCES,
					factory.getProperties(),
					DEFAULT_QUERY_PLAN_MAX_COUNT
			);
		}

		queryPlanCache = new BoundedConcurrentHashMap( maxQueryPlanCount, 20, BoundedConcurrentHashMap.Eviction.LIRS );
		parameterMetadataCache = new BoundedConcurrentHashMap<>(
				maxParameterMetadataCount,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);

		nativeQueryInterpreter = factory.getServiceRegistry().getService( NativeQueryInterpreter.class );
	}

	/**
	 * Obtain the parameter metadata for given native-sql query.
	 * <p/>
	 * for native-sql queries, the param metadata is determined outside any relation to a query plan, because
	 * query plan creation and/or retrieval for a native-sql query depends on all of the return types having been
	 * set, which might not be the case up-front when param metadata would be most useful
	 *
	 * @param query The query
	 * @return The parameter metadata
	 */
	public ParameterMetadata getSQLParameterMetadata(final String query, boolean isOrdinalParameterZeroBased)  {
		final ParameterMetadataKey key = new ParameterMetadataKey( query, isOrdinalParameterZeroBased );
		return parameterMetadataCache.computeIfAbsent( key, k -> nativeQueryInterpreter.getParameterMetadata( query ) );
	}

	/**
	 * Get the query plan for the given HQL query, creating it and caching it if not already cached
	 *
	 * @param queryString The HQL query string
	 * @param shallow Whether the execution will be shallow
	 * @param enabledFilters The filters enabled on the Session
	 *
	 * @return The query plan
	 *
	 * @throws QueryException Indicates a problem translating the query
	 * @throws MappingException Indicates a problem translating the query
	 */
	@SuppressWarnings("unchecked")
	public HQLQueryPlan getHQLQueryPlan(String queryString, boolean shallow, Map<String, Filter> enabledFilters)
			throws QueryException, MappingException {
		final HQLQueryPlanKey key = new HQLQueryPlanKey( queryString, shallow, enabledFilters );
		HQLQueryPlan value = (HQLQueryPlan) queryPlanCache.get( key );
		final StatisticsImplementor statistics = factory.getStatistics();
		boolean stats = statistics.isStatisticsEnabled();

		if ( value == null ) {
			final long startTime = ( stats ) ? System.nanoTime() : 0L;

			LOG.tracev( "Unable to locate HQL query plan in cache; generating ({0})", queryString );
			value = queryPlanCreator.createQueryPlan( queryString, shallow, enabledFilters, factory );

			if ( stats ) {
				final long endTime = System.nanoTime();
				final long microseconds = TimeUnit.MICROSECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
				statistics.queryCompiled( queryString, microseconds );
			}

			queryPlanCache.putIfAbsent( key, value );
		}
		else {
			LOG.tracev( "Located HQL query plan in cache ({0})", queryString );

			if ( stats ) {
				statistics.queryPlanCacheHit( queryString );
			}
		}
		return value;
	}

	/**
	 * Get the query plan for the given collection HQL filter fragment, creating it and caching it if not already cached
	 *
	 * @param filterString The HQL filter fragment
	 * @param collectionRole The collection being filtered
	 * @param shallow Whether the execution will be shallow
	 * @param enabledFilters The filters enabled on the Session
	 *
	 * @return The query plan
	 *
	 * @throws QueryException Indicates a problem translating the query
	 * @throws MappingException Indicates a problem translating the query
	 */
	@SuppressWarnings("unchecked")
	public FilterQueryPlan getFilterQueryPlan(
			String filterString,
			String collectionRole,
			boolean shallow,
			Map<String,Filter> enabledFilters) throws QueryException, MappingException {
		final FilterQueryPlanKey key =  new FilterQueryPlanKey( filterString, collectionRole, shallow, enabledFilters );
		FilterQueryPlan value = (FilterQueryPlan) queryPlanCache.get( key );
		if ( value == null ) {
			LOG.tracev(
					"Unable to locate collection-filter query plan in cache; generating ({0} : {1} )",
					collectionRole,
					filterString
			);
			value = new FilterQueryPlan( filterString, collectionRole, shallow, enabledFilters,factory );
			queryPlanCache.putIfAbsent( key, value );
		}
		else {
			LOG.tracev( "Located collection-filter query plan in cache ({0} : {1})", collectionRole, filterString );
		}
		return value;
	}

	/**
	 * Get the query plan for a native SQL query, creating it and caching it if not already cached
	 *
	 * @param spec The native SQL query specification
	 *
	 * @return The query plan
	 *
	 * @throws QueryException Indicates a problem translating the query
	 * @throws MappingException Indicates a problem translating the query
	 */
	@SuppressWarnings("unchecked")
	public NativeSQLQueryPlan getNativeSQLQueryPlan(final NativeSQLQuerySpecification spec) {
		NativeSQLQueryPlan value = (NativeSQLQueryPlan) queryPlanCache.get( spec );
		if ( value == null ) {
			LOG.tracev( "Unable to locate native-sql query plan in cache; generating ({0})", spec.getQueryString() );
			value = nativeQueryInterpreter.createQueryPlan( spec, factory );
			queryPlanCache.putIfAbsent( spec, value );
		}
		else {
			LOG.tracev( "Located native-sql query plan in cache ({0})", spec.getQueryString() );
		}
		return value;
	}

	/**
	 * Clean up the caches when the SessionFactory is closed.
	 * <p>
	 * Note that depending on the cache strategy implementation chosen, clearing the cache might not reclaim all the
	 * memory.
	 * <p>
	 * Typically, when using LIRS, clearing the cache only invalidates the entries but the outdated entries are kept in
	 * memory until they are replaced by others. It is not considered a memory leak as the cache is bounded.
	 */
	public void cleanup() {
		LOG.trace( "Cleaning QueryPlan Cache" );
		queryPlanCache.clear();
		parameterMetadataCache.clear();
	}

	public NativeQueryInterpreter getNativeQueryInterpreter() {
		return nativeQueryInterpreter;
	}

	private static class ParameterMetadataKey implements Serializable {
		private final String query;
		private final boolean isOrdinalParameterZeroBased;
		private final int hashCode;

		public ParameterMetadataKey(String query, boolean isOrdinalParameterZeroBased) {
			this.query = query;
			this.isOrdinalParameterZeroBased = isOrdinalParameterZeroBased;
			int hash = query.hashCode();
			hash = 29 * hash + ( isOrdinalParameterZeroBased ? 1 : 0 );
			this.hashCode = hash;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final ParameterMetadataKey that = (ParameterMetadataKey) o;

			return isOrdinalParameterZeroBased == that.isOrdinalParameterZeroBased
					&& query.equals( that.query );

		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}

	private static class HQLQueryPlanKey implements Serializable {
		private final String query;
		private final boolean shallow;
		private final Set<DynamicFilterKey> filterKeys;
		private final int hashCode;

		public HQLQueryPlanKey(String query, boolean shallow, Map enabledFilters) {
			this.query = query;
			this.shallow = shallow;
			if ( CollectionHelper.isEmpty( enabledFilters ) ) {
				filterKeys = Collections.emptySet();
			}
			else {
				final Set<DynamicFilterKey> tmp = new HashSet<DynamicFilterKey>(
						CollectionHelper.determineProperSizing( enabledFilters ),
						CollectionHelper.LOAD_FACTOR
				);
				for ( Object o : enabledFilters.values() ) {
					tmp.add( new DynamicFilterKey( (FilterImpl) o ) );
				}
				this.filterKeys = Collections.unmodifiableSet( tmp );
			}

			int hash = query.hashCode();
			hash = 29 * hash + ( shallow ? 1 : 0 );
			hash = 29 * hash + filterKeys.hashCode();
			this.hashCode = hash;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final HQLQueryPlanKey that = (HQLQueryPlanKey) o;

			return shallow == that.shallow
					&& filterKeys.equals( that.filterKeys )
					&& query.equals( that.query );

		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}

	private static class DynamicFilterKey implements Serializable {
		private final String filterName;
		private final Map<String,Integer> parameterMetadata;
		private final int hashCode;

		private DynamicFilterKey(FilterImpl filter) {
			this.filterName = filter.getName();
			final Map<String, ?> parameters = filter.getParameters();
			if ( parameters.isEmpty() ) {
				parameterMetadata = Collections.emptyMap();
			}
			else {
				parameterMetadata = new HashMap<String,Integer>(
						CollectionHelper.determineProperSizing( parameters ),
						CollectionHelper.LOAD_FACTOR
				);
				for ( Object o : parameters.entrySet() ) {
					final Map.Entry entry = (Map.Entry) o;
					final String key = (String) entry.getKey();
					final Integer valueCount;
					if ( Collection.class.isInstance( entry.getValue() ) ) {
						valueCount = ( (Collection) entry.getValue() ).size();
					}
					else {
						valueCount = 1;
					}
					parameterMetadata.put( key, valueCount );
				}
			}

			int hash = filterName.hashCode();
			hash = 31 * hash + parameterMetadata.hashCode();
			this.hashCode = hash;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final DynamicFilterKey that = (DynamicFilterKey) o;
			return filterName.equals( that.filterName )
					&& parameterMetadata.equals( that.parameterMetadata );

		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}

	private static class FilterQueryPlanKey implements Serializable {
		private final String query;
		private final String collectionRole;
		private final boolean shallow;
		private final Set<String> filterNames;
		private final int hashCode;

		@SuppressWarnings({ "unchecked" })
		public FilterQueryPlanKey(String query, String collectionRole, boolean shallow, Map enabledFilters) {
			this.query = query;
			this.collectionRole = collectionRole;
			this.shallow = shallow;

			if ( CollectionHelper.isEmpty( enabledFilters ) ) {
				this.filterNames = Collections.emptySet();
			}
			else {
				final Set<String> tmp = new HashSet<String>( enabledFilters.keySet() );
				this.filterNames = Collections.unmodifiableSet( tmp );

			}

			int hash = query.hashCode();
			hash = 29 * hash + collectionRole.hashCode();
			hash = 29 * hash + ( shallow ? 1 : 0 );
			hash = 29 * hash + filterNames.hashCode();
			this.hashCode = hash;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final FilterQueryPlanKey that = (FilterQueryPlanKey) o;
			return shallow == that.shallow
					&& filterNames.equals( that.filterNames )
					&& query.equals( that.query )
					&& collectionRole.equals( that.collectionRole );

		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}
}
