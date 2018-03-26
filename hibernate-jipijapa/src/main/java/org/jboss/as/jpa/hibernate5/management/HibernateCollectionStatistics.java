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
import org.hibernate.stat.CollectionStatistics;

import org.jipijapa.management.spi.EntityManagerFactoryAccess;
import org.jipijapa.management.spi.Operation;
import org.jipijapa.management.spi.PathAddress;

/**
 * Hibernate collection statistics
 *
 * @author Scott Marlow
 */
public class HibernateCollectionStatistics extends HibernateAbstractStatistics {

	private static final String ATTRIBUTE_COLLECTION_NAME = "collection-name";
	public static final String OPERATION_COLLECTION_LOAD_COUNT = "collection-load-count";
	public static final String OPERATION_COLLECTION_FETCH_COUNT = "collection-fetch-count";
	public static final String OPERATION_COLLECTION_UPDATE_COUNT = "collection-update-count";
	public static final String OPERATION_COLLECTION_REMOVE_COUNT = "collection-remove-count";
	public static final String OPERATION_COLLECTION_RECREATED_COUNT = "collection-recreated-count";

	public HibernateCollectionStatistics() {
		/**
		 * specify the different operations
		 */
		getOperations().put( ATTRIBUTE_COLLECTION_NAME, showCollectionName );
		getTypes().put( ATTRIBUTE_COLLECTION_NAME, String.class );

		getOperations().put( OPERATION_COLLECTION_LOAD_COUNT, collectionLoadCount );
		getTypes().put( OPERATION_COLLECTION_LOAD_COUNT, Long.class );

		getOperations().put( OPERATION_COLLECTION_FETCH_COUNT, collectionFetchCount );
		getTypes().put( OPERATION_COLLECTION_FETCH_COUNT, Long.class );

		getOperations().put( OPERATION_COLLECTION_UPDATE_COUNT, collectionUpdateCount );
		getTypes().put( OPERATION_COLLECTION_UPDATE_COUNT, Long.class );

		getOperations().put( OPERATION_COLLECTION_REMOVE_COUNT, collectionRemoveCount );
		getTypes().put( OPERATION_COLLECTION_REMOVE_COUNT, Long.class );

		getOperations().put( OPERATION_COLLECTION_RECREATED_COUNT, collectionRecreatedCount );
		getTypes().put( OPERATION_COLLECTION_RECREATED_COUNT, Long.class );
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
		return Collections.unmodifiableCollection( Arrays.asList( stats.getCollectionRoleNames() ) );
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

	private CollectionStatistics getStatistics(final EntityManagerFactory entityManagerFactory, String collectionName) {
		if ( entityManagerFactory == null ) {
			return null;
		}
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		if ( sessionFactory != null ) {
			return sessionFactory.getStatistics().getCollectionStatistics( collectionName );
		}
		return null;
	}

	private Operation showCollectionName = new Operation() {
		@Override
		public Object invoke(Object... args) {
			return getStatisticName( args );
		}
	};

	private Operation collectionUpdateCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			CollectionStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getUpdateCount() : 0 );
		}
	};

	private Operation collectionRemoveCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			CollectionStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getRemoveCount() : 0 );
		}
	};

	private Operation collectionRecreatedCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			CollectionStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getRemoveCount() : 0 );
		}
	};

	private Operation collectionLoadCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			CollectionStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getLoadCount() : 0 );
		}
	};

	private Operation collectionFetchCount = new Operation() {
		@Override
		public Object invoke(Object... args) {
			CollectionStatistics statistics = getStatistics(
					getEntityManagerFactory( args ),
					getStatisticName( args )
			);
			return Long.valueOf( statistics != null ? statistics.getFetchCount() : 0 );
		}
	};
}
