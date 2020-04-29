/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static <R> RowReader<R> createRowReader(
			SessionFactoryImplementor sessionFactory,
			Callback callback,
			RowTransformer<R> rowTransformer,
			JdbcValues jdbcValues) {
		final List<Initializer> initializers = new ArrayList<>();

		final List<DomainResultAssembler> assemblers = jdbcValues.getValuesMapping().resolveAssemblers(
				getInitializerConsumer( initializers ),
				() -> sessionFactory
		);

		return new StandardRowReader<>(
				assemblers,
				initializers,
				rowTransformer,
				callback
		);
	}

	private static Consumer<Initializer> getInitializerConsumer(List<Initializer> initializers) {
		return initializer -> {
			ResultsLogger.INSTANCE.debugf( "Initializer registration : %s", initializer );
			if ( initializers.contains( initializer ) ) {
				ResultsLogger.INSTANCE.debug( "Skipping initializer registration - already registered" );
			}

			ResultsLogger.INSTANCE.debugf( "Adding initializer : %s", initializer );
			initializers.add( initializer );
		};
	}

	public static void finalizeCollectionLoading(
			PersistenceContext persistenceContext,
			CollectionPersister collectionDescriptor,
			PersistentCollection collectionInstance,
			Object key) {
		CollectionEntry collectionEntry = persistenceContext.getCollectionEntry( collectionInstance );
		if ( collectionEntry == null ) {
			collectionEntry = persistenceContext.addInitializedCollection(
					collectionDescriptor,
					collectionInstance,
					key
			);
		}
		else {
			collectionEntry.postInitialize( collectionInstance );
		}

		if ( collectionDescriptor.getCollectionType().hasHolder() ) {
			persistenceContext.addCollectionHolder( collectionInstance );
		}

		final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();
		batchFetchQueue.removeBatchLoadableCollection( collectionEntry );

		// todo (6.0) : there is other logic still needing to be implemented here.  caching, etc
		// 		see org.hibernate.engine.loading.internal.CollectionLoadContext#endLoadingCollection in 5.x
	}
}
