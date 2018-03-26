/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.management;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate entity cache (SecondLevelCacheStatistics) statistics
 *
 * @author Scott Marlow
 */
public class HibernateEntityCacheStatistics extends HibernateAbstractStatistics {

	public static final String ATTRIBUTE_ENTITY_CACHE_REGION_NAME = "entity-cache-region-name";
	public static final String OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT = "second-level-cache-hit-count";
	public static final String OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT = "second-level-cache-miss-count";
	public static final String OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT = "second-level-cache-put-count";
	public static final String OPERATION_SECOND_LEVEL_CACHE_COUNT_IN_MEMORY = "second-level-cache-count-in-memory";
	public static final String OPERATION_SECOND_LEVEL_CACHE_SIZE_IN_MEMORY = "second-level-cache-size-in-memory";

	public HibernateEntityCacheStatistics() {
		/**
		 * specify the different operations
		 */
		getOperations().put( ATTRIBUTE_ENTITY_CACHE_REGION_NAME, getEntityCacheRegionName );
		getTypes().put( ATTRIBUTE_ENTITY_CACHE_REGION_NAME, String.class );

		getOperations().put( OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT, entityCacheHitCount );
		getTypes().put( OPERATION_SECOND_LEVEL_CACHE_HIT_COUNT, Long.class );

		getOperations().put( OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT, entityCacheMissCount );
		getTypes().put( OPERATION_SECOND_LEVEL_CACHE_MISS_COUNT, Long.class );

		getOperations().put( OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT, entityCachePutCount );
		getTypes().put( OPERATION_SECOND_LEVEL_CACHE_PUT_COUNT, Long.class );

		getOperations().put( OPERATION_SECOND_LEVEL_CACHE_COUNT_IN_MEMORY, entityCacheCountInMemory );
		getTypes().put( OPERATION_SECOND_LEVEL_CACHE_COUNT_IN_MEMORY, Long.class );

		getOperations().put( OPERATION_SECOND_LEVEL_CACHE_SIZE_IN_MEMORY, entityCacheSizeInMemory );
		getTypes().put( OPERATION_SECOND_LEVEL_CACHE_SIZE_IN_MEMORY, Long.class );

	}

	@Override
	public Collection<String> getDynamicChildrenNames(
			EntityManagerFactoryAccess entityManagerFactoryLookup,
			PathAddress pathAddress) {
		org.hibernate.stat.Statistics stats = getBaseStatistics( entityManagerFactoryLookup.entityManagerFactory(
				pathAddress.getValue( HibernateStatistics.PROVIDER_LABEL ) ) );
		if ( stats == null ) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableCollection( Arrays.asList( stats.getEntityNames() ) );
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

	org.hibernate.stat.SecondLevelCacheStatistics getStatistics(
			EntityManagerFactoryAccess entityManagerFactoryaccess,
			PathAddress pathAddress) {
		String scopedPersistenceUnitName = pathAddress.getValue( HibernateStatistics.PROVIDER_LABEL );
		SessionFactory sessionFactory = entityManagerFactoryaccess.entityManagerFactory( scopedPersistenceUnitName )
				.unwrap( SessionFactory.class );
		if ( sessionFactory != null ) {
			// The entity class name is prefixed by the application scoped persistence unit name

			return sessionFactory.getStatistics().getSecondLevelCacheStatistics(
					scopedPersistenceUnitName + "." + pathAddress.getValue( HibernateStatistics.ENTITYCACHE )
			);
		}
		return null;
	}

	private Operation getEntityCacheRegionName = new Operation() {
		@Override
		public Object invoke(Object... args) {
			return getStatisticName( args );
		}
	};


	private Operation entityCacheHitCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics( getEntityManagerFactoryAccess(
					args ), getPathAddress( args ) );
			return Long.valueOf( statistics != null ? statistics.getHitCount() : 0 );
		}
	};

	private Operation entityCacheMissCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics( getEntityManagerFactoryAccess(
					args ), getPathAddress( args ) );
			return Long.valueOf( statistics != null ? statistics.getMissCount() : 0 );
		}
	};

	private Operation entityCachePutCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics( getEntityManagerFactoryAccess(
					args ), getPathAddress( args ) );
			return Long.valueOf( statistics != null ? statistics.getPutCount() : 0 );
		}
	};

	private Operation entityCacheSizeInMemory = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics( getEntityManagerFactoryAccess(
					args ), getPathAddress( args ) );
			return Long.valueOf( statistics != null ? statistics.getSizeInMemory() : 0 );
		}
	};

	private Operation entityCacheCountInMemory = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.SecondLevelCacheStatistics statistics = getStatistics( getEntityManagerFactoryAccess(
					args ), getPathAddress( args ) );
			return Long.valueOf( statistics != null ? statistics.getElementCountInMemory() : 0 );
		}
	};

}
