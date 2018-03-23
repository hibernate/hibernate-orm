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
import java.util.Set;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;

import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate entity statistics
 *
 * @author Scott Marlow
 */
public class HibernateEntityStatistics extends HibernateAbstractStatistics {

	public static final String OPERATION_ENTITY_DELETE_COUNT = "entity-delete-count";
	public static final String OPERATION_ENTITY_INSERT_COUNT = "entity-insert-count";
	public static final String OPERATION_ENTITY_LOAD_COUNT = "entity-load-count";
	public static final String OPERATION_ENTITY_FETCH_COUNT = "entity-fetch-count";
	public static final String OPERATION_ENTITY_UPDATE_COUNT = "entity-update-count";
	public static final String OPERATION_OPTIMISTIC_FAILURE_COUNT = "optimistic-failure-count";

	public HibernateEntityStatistics() {
		/**
		 * specify the different operations
		 */
		getOperations().put( OPERATION_ENTITY_DELETE_COUNT, entityDeleteCount );
		getTypes().put( OPERATION_ENTITY_DELETE_COUNT, Long.class );

		getOperations().put( OPERATION_ENTITY_INSERT_COUNT, entityInsertCount );
		getTypes().put( OPERATION_ENTITY_INSERT_COUNT, Long.class );

		getOperations().put( OPERATION_ENTITY_LOAD_COUNT, entityLoadCount );
		getTypes().put( OPERATION_ENTITY_LOAD_COUNT, Long.class );

		getOperations().put( OPERATION_ENTITY_FETCH_COUNT, entityFetchCount );
		getTypes().put( OPERATION_ENTITY_FETCH_COUNT, Long.class );

		getOperations().put( OPERATION_ENTITY_UPDATE_COUNT, entityUpdateCount );
		getTypes().put( OPERATION_ENTITY_UPDATE_COUNT, Long.class );

		getOperations().put( OPERATION_OPTIMISTIC_FAILURE_COUNT, optimisticFailureCount );
		getTypes().put( OPERATION_OPTIMISTIC_FAILURE_COUNT, Long.class );
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

	private org.hibernate.stat.EntityStatistics getStatistics(
			EntityManagerFactory entityManagerFactory,
			String entityName) {
		if ( entityManagerFactory == null ) {
			return null;
		}
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		if ( sessionFactory != null ) {
			return sessionFactory.getStatistics().getEntityStatistics( entityName );
		}
		return null;
	}

	private Operation entityDeleteCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.EntityStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getDeleteCount() : 0 );
		}
	};

	private Operation entityFetchCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.EntityStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getFetchCount() : 0 );
		}
	};

	private Operation entityInsertCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.EntityStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getInsertCount() : 0 );
		}
	};

	private Operation entityLoadCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.EntityStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getLoadCount() : 0 );
		}
	};

	private Operation entityUpdateCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.EntityStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getUpdateCount() : 0 );
		}
	};

	private Operation optimisticFailureCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			org.hibernate.stat.EntityStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getOptimisticFailureCount() : 0 );
		}
	};

	@Override
	public Set<String> getNames() {

		return Collections.unmodifiableSet( getOperations().keySet() );
	}

	@Override
	public Collection<String> getDynamicChildrenNames(
			EntityManagerFactoryAccess entityManagerFactoryLookup,
			PathAddress pathAddress) {
		org.hibernate.stat.Statistics statistics = getBaseStatistics( entityManagerFactoryLookup.entityManagerFactory(
				pathAddress.getValue( HibernateStatistics.PROVIDER_LABEL ) ) );
		return statistics != null ?
				Collections.unmodifiableCollection( Arrays.asList( statistics.getEntityNames() ) ) :
				Collections.EMPTY_LIST;

	}
}
