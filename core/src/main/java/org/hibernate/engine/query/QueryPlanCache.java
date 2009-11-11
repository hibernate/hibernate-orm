/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine.query;

import org.hibernate.util.SimpleMRUCache;
import org.hibernate.util.SoftLimitMRUCache;
import org.hibernate.util.CollectionHelper;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.QueryException;
import org.hibernate.MappingException;
import org.hibernate.impl.FilterImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Collection;

/**
 * Acts as a cache for compiled query plans, as well as query-parameter metadata.
 *
 * @author Steve Ebersole
 */
public class QueryPlanCache implements Serializable {

	private static final Logger log = LoggerFactory.getLogger( QueryPlanCache.class );

	private SessionFactoryImplementor factory;

	public QueryPlanCache(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	// simple cache of param metadata based on query string.  Ideally, the
	// original "user-supplied query" string should be used to retreive this
	// metadata (i.e., not the para-list-expanded query string) to avoid
	// unnecessary cache entries.
	// Used solely for caching param metadata for native-sql queries, see
	// getSQLParameterMetadata() for a discussion as to why...
	private final SimpleMRUCache sqlParamMetadataCache = new SimpleMRUCache();

	// the cache of the actual plans...
	private final SoftLimitMRUCache planCache = new SoftLimitMRUCache( 128 );


	public ParameterMetadata getSQLParameterMetadata(String query) {
		ParameterMetadata metadata = ( ParameterMetadata ) sqlParamMetadataCache.get( query );
		if ( metadata == null ) {
			// for native-sql queries, the param metadata is determined outside
			// any relation to a query plan, because query plan creation and/or
			// retreival for a native-sql query depends on all of the return
			// types having been set, which might not be the case up-front when
			// param metadata would be most useful
			metadata = buildNativeSQLParameterMetadata( query );
			sqlParamMetadataCache.put( query, metadata );
		}
		return metadata;
	}

	public HQLQueryPlan getHQLQueryPlan(String queryString, boolean shallow, Map enabledFilters)
			throws QueryException, MappingException {
		HQLQueryPlanKey key = new HQLQueryPlanKey( queryString, shallow, enabledFilters );
		HQLQueryPlan plan = ( HQLQueryPlan ) planCache.get ( key );

		if ( plan == null ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "unable to locate HQL query plan in cache; generating (" + queryString + ")" );
			}
			plan = new HQLQueryPlan(queryString, shallow, enabledFilters, factory );
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.trace( "located HQL query plan in cache (" + queryString + ")" );
			}
		}

		planCache.put( key, plan );

		return plan;
	}

	public FilterQueryPlan getFilterQueryPlan(String filterString, String collectionRole, boolean shallow, Map enabledFilters)
			throws QueryException, MappingException {
		FilterQueryPlanKey key = new FilterQueryPlanKey( filterString, collectionRole, shallow, enabledFilters );
		FilterQueryPlan plan = ( FilterQueryPlan ) planCache.get ( key );

		if ( plan == null ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "unable to locate collection-filter query plan in cache; generating (" + collectionRole + " : " + filterString + ")" );
			}
			plan = new FilterQueryPlan( filterString, collectionRole, shallow, enabledFilters, factory );
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.trace( "located collection-filter query plan in cache (" + collectionRole + " : " + filterString + ")" );
			}
		}

		planCache.put( key, plan );

		return plan;
	}

	public NativeSQLQueryPlan getNativeSQLQueryPlan(NativeSQLQuerySpecification spec) {
		NativeSQLQueryPlan plan = ( NativeSQLQueryPlan ) planCache.get( spec );

		if ( plan == null ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "unable to locate native-sql query plan in cache; generating (" + spec.getQueryString() + ")" );
			}
			plan = new NativeSQLQueryPlan( spec, factory );
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.trace( "located native-sql query plan in cache (" + spec.getQueryString() + ")" );
			}
		}

		planCache.put( spec, plan );
		return plan;
	}

	private ParameterMetadata buildNativeSQLParameterMetadata(String sqlString) {
		ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( sqlString );

		OrdinalParameterDescriptor[] ordinalDescriptors =
				new OrdinalParameterDescriptor[ recognizer.getOrdinalParameterLocationList().size() ];
		for ( int i = 0; i < recognizer.getOrdinalParameterLocationList().size(); i++ ) {
			final Integer position = ( Integer ) recognizer.getOrdinalParameterLocationList().get( i );
			ordinalDescriptors[i] = new OrdinalParameterDescriptor( i, null, position.intValue() );
		}

		Iterator itr = recognizer.getNamedParameterDescriptionMap().entrySet().iterator();
		Map namedParamDescriptorMap = new HashMap();
		while( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final String name = ( String ) entry.getKey();
			final ParamLocationRecognizer.NamedParameterDescription description =
					( ParamLocationRecognizer.NamedParameterDescription ) entry.getValue();
			namedParamDescriptorMap.put(
					name ,
			        new NamedParameterDescriptor( name, null, description.buildPositionsArray(), description.isJpaStyle() )
			);
		}

		return new ParameterMetadata( ordinalDescriptors, namedParamDescriptorMap );
	}

	private static class HQLQueryPlanKey implements Serializable {
		private final String query;
		private final boolean shallow;
		private final Set filterKeys;
		private final int hashCode;

		public HQLQueryPlanKey(String query, boolean shallow, Map enabledFilters) {
			this.query = query;
			this.shallow = shallow;

			if ( enabledFilters == null || enabledFilters.isEmpty() ) {
				filterKeys = Collections.EMPTY_SET;
			}
			else {
				Set tmp = new HashSet(
						CollectionHelper.determineProperSizing( enabledFilters ),
						CollectionHelper.LOAD_FACTOR
				);
				Iterator itr = enabledFilters.values().iterator();
				while ( itr.hasNext() ) {
					tmp.add( new DynamicFilterKey( ( FilterImpl ) itr.next() ) );
				}
				this.filterKeys = Collections.unmodifiableSet( tmp );
			}

			int hash = query.hashCode();
			hash = 29 * hash + ( shallow ? 1 : 0 );
			hash = 29 * hash + filterKeys.hashCode();
			this.hashCode = hash;
		}

		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final HQLQueryPlanKey that = ( HQLQueryPlanKey ) o;

			return shallow == that.shallow
					&& filterKeys.equals( that.filterKeys )
					&& query.equals( that.query );

		}

		public int hashCode() {
			return hashCode;
		}
	}

	private static class DynamicFilterKey implements Serializable {
		private final String filterName;
		private final Map parameterMetadata;
		private final int hashCode;

		private DynamicFilterKey(FilterImpl filter) {
			this.filterName = filter.getName();
			if ( filter.getParameters().isEmpty() ) {
				parameterMetadata = Collections.EMPTY_MAP;
			}
			else {
				parameterMetadata = new HashMap(
						CollectionHelper.determineProperSizing( filter.getParameters() ),
						CollectionHelper.LOAD_FACTOR
				);
				Iterator itr = filter.getParameters().entrySet().iterator();
				while ( itr.hasNext() ) {
					final Integer valueCount;
					final Map.Entry entry = ( Map.Entry ) itr.next();
					if ( Collection.class.isInstance( entry.getValue() ) ) {
						valueCount = new Integer( ( (Collection) entry.getValue() ).size() );
					}
					else {
						valueCount = new Integer(1);
					}
					parameterMetadata.put( entry.getKey(), valueCount );
				}
			}

			int hash = filterName.hashCode();
			hash = 31 * hash + parameterMetadata.hashCode();
			this.hashCode = hash;
		}

		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			DynamicFilterKey that = ( DynamicFilterKey ) o;

			return filterName.equals( that.filterName )
					&& parameterMetadata.equals( that.parameterMetadata );

		}

		public int hashCode() {
			return hashCode;
		}
	}

	private static class FilterQueryPlanKey implements Serializable {
		private final String query;
		private final String collectionRole;
		private final boolean shallow;
		private final Set filterNames;
		private final int hashCode;

		public FilterQueryPlanKey(String query, String collectionRole, boolean shallow, Map enabledFilters) {
			this.query = query;
			this.collectionRole = collectionRole;
			this.shallow = shallow;

			if ( enabledFilters == null || enabledFilters.isEmpty() ) {
				filterNames = Collections.EMPTY_SET;
			}
			else {
				Set tmp = new HashSet();
				tmp.addAll( enabledFilters.keySet() );
				this.filterNames = Collections.unmodifiableSet( tmp );
			}

			int hash = query.hashCode();
			hash = 29 * hash + collectionRole.hashCode();
			hash = 29 * hash + ( shallow ? 1 : 0 );
			hash = 29 * hash + filterNames.hashCode();
			this.hashCode = hash;
		}

		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final FilterQueryPlanKey that = ( FilterQueryPlanKey ) o;

			return shallow == that.shallow
					&& filterNames.equals( that.filterNames )
					&& query.equals( that.query )
					&& collectionRole.equals( that.collectionRole );

		}

		public int hashCode() {
			return hashCode;
		}
	}
}
