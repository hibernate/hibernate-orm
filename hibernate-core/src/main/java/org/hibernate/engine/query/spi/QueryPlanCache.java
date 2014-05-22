/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	/**
	 * The default strong reference count.
	 */
	public static final int DEFAULT_PARAMETER_METADATA_MAX_COUNT = 128;
	/**
	 * The default soft reference count.
	 */
	public static final int DEFAULT_QUERY_PLAN_MAX_COUNT = 2048;

	private final SessionFactoryImplementor factory;

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
	private final BoundedConcurrentHashMap<String,ParameterMetadata> parameterMetadataCache;


	private NativeQueryInterpreter nativeQueryInterpreterService;

	/**
	 * Constructs the QueryPlanCache to be used by the given SessionFactory
	 *
	 * @param factory The SessionFactory
	 */
	@SuppressWarnings("deprecation")
	public QueryPlanCache(final SessionFactoryImplementor factory) {
		this.factory = factory;

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
		parameterMetadataCache = new BoundedConcurrentHashMap<String, ParameterMetadata>(
				maxParameterMetadataCount,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);

		nativeQueryInterpreterService = factory.getServiceRegistry().getService( NativeQueryInterpreter.class );
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
	public ParameterMetadata getSQLParameterMetadata(final String query)  {
		ParameterMetadata value = parameterMetadataCache.get( query );
		if ( value == null ) {
			value = nativeQueryInterpreterService.getParameterMetadata( query );
			parameterMetadataCache.putIfAbsent( query, value );
		}
		return value;
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
	public HQLQueryPlan getHQLQueryPlan(String queryString, boolean shallow, Map<String,Filter> enabledFilters)
			throws QueryException, MappingException {
		final HQLQueryPlanKey key = new HQLQueryPlanKey( queryString, shallow, enabledFilters );
		HQLQueryPlan value = (HQLQueryPlan) queryPlanCache.get( key );
		if ( value == null ) {
			LOG.tracev( "Unable to locate HQL query plan in cache; generating ({0})", queryString );
			value = new HQLQueryPlan( queryString, shallow, enabledFilters, factory );
			queryPlanCache.putIfAbsent( key, value );
		} else {
			LOG.tracev( "Located HQL query plan in cache ({0})", queryString );
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
			value = nativeQueryInterpreterService.createQueryPlan( spec, factory );
			queryPlanCache.putIfAbsent( spec, value );
		}
		else {
			LOG.tracev( "Located native-sql query plan in cache ({0})", spec.getQueryString() );
		}
		return value;
	}

	/**
	 * clean up QueryPlanCache when SessionFactory is closed
	 */
	public void cleanup() {
		LOG.trace( "Cleaning QueryPlan Cache" );
		queryPlanCache.clear();
		parameterMetadataCache.clear();
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
			if ( filter.getParameters().isEmpty() ) {
				parameterMetadata = Collections.emptyMap();
			}
			else {
				parameterMetadata = new HashMap<String,Integer>(
						CollectionHelper.determineProperSizing( filter.getParameters() ),
						CollectionHelper.LOAD_FACTOR
				);
				for ( Object o : filter.getParameters().entrySet() ) {
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
				final Set<String> tmp = new HashSet<String>();
				tmp.addAll( enabledFilters.keySet() );
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
