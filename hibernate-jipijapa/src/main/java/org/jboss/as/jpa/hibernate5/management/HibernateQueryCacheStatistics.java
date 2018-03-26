/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.management;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate query cache statistics
 *
 * @author Scott Marlow
 */
public class HibernateQueryCacheStatistics extends HibernateAbstractStatistics {

	public static final String ATTRIBUTE_QUERY_NAME = "query-name";
	public static final String OPERATION_QUERY_EXECUTION_COUNT = "query-execution-count";
	public static final String OPERATION_QUERY_EXECUTION_ROW_COUNT = "query-execution-row-count";
	public static final String OPERATION_QUERY_EXECUTION_AVG_TIME = "query-execution-average-time";
	public static final String OPERATION_QUERY_EXECUTION_MAX_TIME = "query-execution-max-time";
	public static final String OPERATION_QUERY_EXECUTION_MIN_TIME = "query-execution-min-time";
	public static final String OPERATION_QUERY_CACHE_HIT_COUNT = "query-cache-hit-count";
	public static final String OPERATION_QUERY_CACHE_MISS_COUNT = "query-cache-miss-count";
	public static final String OPERATION_QUERY_CACHE_PUT_COUNT = "query-cache-put-count";

	public HibernateQueryCacheStatistics() {
		/**
		 * specify the different operations
		 */
		getOperations().put( ATTRIBUTE_QUERY_NAME, showQueryName );
		getTypes().put( ATTRIBUTE_QUERY_NAME, String.class );

		getOperations().put( OPERATION_QUERY_EXECUTION_COUNT, queryExecutionCount );
		getTypes().put( OPERATION_QUERY_EXECUTION_COUNT, Long.class );

		getOperations().put( OPERATION_QUERY_EXECUTION_ROW_COUNT, queryExecutionRowCount );
		getTypes().put( OPERATION_QUERY_EXECUTION_ROW_COUNT, Long.class );

		getOperations().put( OPERATION_QUERY_EXECUTION_AVG_TIME, queryExecutionAverageTime );
		getTypes().put( OPERATION_QUERY_EXECUTION_AVG_TIME, Long.class );

		getOperations().put( OPERATION_QUERY_EXECUTION_MAX_TIME, queryExecutionMaximumTime );
		getTypes().put( OPERATION_QUERY_EXECUTION_MAX_TIME, Long.class );

		getOperations().put( OPERATION_QUERY_EXECUTION_MIN_TIME, queryExecutionMinimumTime );
		getTypes().put( OPERATION_QUERY_EXECUTION_MIN_TIME, Long.class );

		getOperations().put( OPERATION_QUERY_CACHE_HIT_COUNT, queryCacheHitCount );
		getTypes().put( OPERATION_QUERY_CACHE_HIT_COUNT, Long.class );

		getOperations().put( OPERATION_QUERY_CACHE_MISS_COUNT, queryCacheMissCount );
		getTypes().put( OPERATION_QUERY_CACHE_MISS_COUNT, Long.class );

		getOperations().put( OPERATION_QUERY_CACHE_PUT_COUNT, queryCachePutCount );
		getTypes().put( OPERATION_QUERY_CACHE_PUT_COUNT, Long.class );
	}

	@Override
	public Collection<String> getDynamicChildrenNames(
			EntityManagerFactoryAccess entityManagerFactoryLookup,
			PathAddress pathAddress) {
		Set<String> result = new HashSet<>();
		org.hibernate.stat.Statistics stats = getBaseStatistics( entityManagerFactoryLookup.entityManagerFactory(
				pathAddress.getValue( HibernateStatistics.PROVIDER_LABEL ) ) );
		if ( stats != null ) {
			String[] queries = stats.getQueries();
			if ( queries != null ) {
				for ( String query : queries ) {
					result.add( QueryName.queryName( query ).getDisplayName() );
				}
			}
		}
		return result;
	}

	private org.hibernate.stat.Statistics getBaseStatistics(EntityManagerFactory entityManagerFactory) {
		if ( entityManagerFactory == null ) {
			return null;
		}
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		if ( sessionFactory != null ) {
			return sessionFactory.getStatistics();
		}
		return null;
	}

	private org.hibernate.stat.QueryStatistics getStatistics(
			EntityManagerFactory entityManagerFactory,
			String displayQueryName) {
		if ( entityManagerFactory == null ) {
			return null;
		}
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		// convert displayed (transformed by QueryNames) query name to original query name to look up query statistics
		if ( sessionFactory != null ) {
			String[] originalQueryNames = sessionFactory.getStatistics().getQueries();
			if ( originalQueryNames != null ) {
				for ( String originalQueryName : originalQueryNames ) {
					if ( QueryName.queryName( originalQueryName ).getDisplayName().equals( displayQueryName ) ) {
						return sessionFactory.getStatistics().getQueryStatistics( originalQueryName );
					}
				}
			}
		}
		return null;
	}

	private Operation queryExecutionCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getExecutionCount() : 0 );
		}
	};

	private Operation queryExecutionMaximumTime = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getExecutionMaxTime() : 0 );
		}
	};

	private Operation queryExecutionRowCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getExecutionRowCount() : 0 );
		}
	};

	private Operation queryExecutionAverageTime = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getExecutionAvgTime() : 0 );
		}
	};

	private Operation queryExecutionMinimumTime = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getExecutionMinTime() : 0 );
		}
	};

	private Operation queryCacheHitCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getCacheHitCount() : 0 );
		}
	};

	private Operation queryCacheMissCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getCacheMissCount() : 0 );
		}
	};

	private Operation queryCachePutCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.QueryStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getQueryName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getCachePutCount() : 0 );
		}
	};

	private Operation showQueryName = new Operation() {
		@Override
		public Object invoke(Object... args) {
			String displayQueryName = getQueryName( args );
			EntityManagerFactory entityManagerFactory = getEntityManagerFactory( args );
			if ( displayQueryName != null && entityManagerFactory != null ) {
				SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
				// convert displayed (transformed by QueryNames) query name to original query name
				if ( sessionFactory != null ) {
					String[] originalQueryNames = sessionFactory.getStatistics().getQueries();
					if ( originalQueryNames != null ) {
						for ( String originalQueryName : originalQueryNames ) {
							if ( QueryName.queryName( originalQueryName )
									.getDisplayName()
									.equals( displayQueryName ) ) {
								return originalQueryName;
							}
						}
					}
				}

			}
			return null;
		}
	};

	private String getQueryName(Object... args) {
		PathAddress pathAddress = getPathAddress( args );
		if ( pathAddress != null ) {
			return pathAddress.getValue( HibernateStatistics.QUERYCACHE );
		}
		return null;
	}
}
