/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.collection.internal.ArrayInitializer;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * @author Steve Ebersole
 */
public class JdbcValuesSourceProcessingStateStandardImpl implements JdbcValuesSourceProcessingState {

	private final ExecutionContext executionContext;
	private final JdbcValuesSourceProcessingOptions processingOptions;

	private final BiConsumer<EntityKey,LoadingEntityEntry> loadingEntityEntryConsumer;

	private Map<EntityKey, LoadingEntityEntry> loadingEntityMap;
	private Map<EntityUniqueKey, Initializer> initializerByUniquKeyMap;
	private Map<CollectionKey, LoadingCollectionEntry> loadingCollectionMap;
	private List<CollectionInitializer> arrayInitializers;

	private final PreLoadEvent preLoadEvent;
	private final PostLoadEvent postLoadEvent;

	public JdbcValuesSourceProcessingStateStandardImpl(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions,
			BiConsumer<EntityKey,LoadingEntityEntry> loadingEntityEntryListener) {
		this.executionContext = executionContext;
		this.processingOptions = processingOptions;
		this.loadingEntityEntryConsumer = loadingEntityEntryListener;

		if ( executionContext.getSession().isEventSource() ) {
			final EventSource eventSource = executionContext.getSession().asEventSource();
			preLoadEvent = new PreLoadEvent( eventSource );
			postLoadEvent = new PostLoadEvent( eventSource );
		}
		else {
			preLoadEvent = null;
			postLoadEvent = null;
		}
	}

	@Override
	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return executionContext.getQueryOptions();
	}

	@Override
	public JdbcValuesSourceProcessingOptions getProcessingOptions() {
		return processingOptions;
	}

	@Override
	public PreLoadEvent getPreLoadEvent() {
		return preLoadEvent;
	}

	@Override
	public PostLoadEvent getPostLoadEvent() {
		return postLoadEvent;
	}

	@Override
	public void registerLoadingEntity(
			EntityKey entityKey,
			LoadingEntityEntry loadingEntry) {
		if ( loadingEntityMap == null ) {
			loadingEntityMap = new HashMap<>();
		}

		if ( loadingEntityEntryConsumer != null ) {
			loadingEntityEntryConsumer.accept( entityKey, loadingEntry );
		}

		loadingEntityMap.put( entityKey, loadingEntry );
	}

	@Override
	public void registerInitializer(EntityUniqueKey entityKey, Initializer initializer) {
		if ( initializerByUniquKeyMap == null ) {
			initializerByUniquKeyMap = new HashMap<>();
		}
		initializerByUniquKeyMap.put( entityKey, initializer );
	}

	@Override
	public Initializer findInitializer(EntityUniqueKey entityKey) {
		return initializerByUniquKeyMap == null ? null : initializerByUniquKeyMap.get( entityKey );
	}

	@Override
	public LoadingEntityEntry findLoadingEntityLocally(EntityKey entityKey) {
		return loadingEntityMap == null ? null : loadingEntityMap.get( entityKey );
	}

	@Override
	public LoadingCollectionEntry findLoadingCollectionLocally(CollectionKey key) {
		if ( loadingCollectionMap == null ) {
			return null;
		}

		return loadingCollectionMap.get( key );
	}

	@Override
	public void registerLoadingCollection(CollectionKey key, LoadingCollectionEntry loadingCollectionEntry) {
		if ( loadingCollectionMap == null ) {
			loadingCollectionMap = new HashMap<>();
		}

		loadingCollectionMap.put( key, loadingCollectionEntry );
		if ( loadingCollectionEntry.getInitializer() instanceof ArrayInitializer ) {
			if ( arrayInitializers == null ) {
				arrayInitializers = new ArrayList<>();
			}
			arrayInitializers.add( loadingCollectionEntry.getInitializer() );
		}
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return executionContext.getSession();
	}

	@Override
	public void finishUp() {
		// now we can finalize loading collections
		finishLoadingCollections();

		postLoad();
	}

	private void postLoad() {
		if ( loadingEntityMap == null ) {
			return;
		}
		final EventListenerGroup<PostLoadEventListener> listenerGroup = executionContext.getSession().getFactory()
				.getFastSessionServices()
				.eventListenerGroup_POST_LOAD;

		loadingEntityMap.forEach(
				(entityKey, loadingEntityEntry) -> {
					if ( loadingEntityEntry.getEntityInstance() != null ) {
						if ( postLoadEvent != null ) {
							postLoadEvent.reset();
							postLoadEvent.setEntity( loadingEntityEntry.getEntityInstance() )
									.setId( entityKey.getIdentifier() )
									.setPersister( loadingEntityEntry.getDescriptor() );
							listenerGroup.fireEventOnEachListener( postLoadEvent, PostLoadEventListener::onPostLoad );
						}

						final Callback callback = executionContext.getCallback();
						if ( callback != null ) {
							callback.invokeAfterLoadActions(
									loadingEntityEntry.getEntityInstance(),
									loadingEntityEntry.getDescriptor(),
									getSession()
							);
						}
					}
				}
		);
		loadingEntityMap = null;
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isReadOnly() {
		if ( getQueryOptions().isReadOnly() != null ) {
			return getQueryOptions().isReadOnly();
		}

		if ( executionContext.getSession() instanceof EventSource ) {
			return executionContext.getSession().isDefaultReadOnly();
		}

		return false;
	}


	private void finishLoadingCollections() {
		if ( loadingCollectionMap != null ) {
			for ( LoadingCollectionEntry loadingCollectionEntry : loadingCollectionMap.values() ) {
				loadingCollectionEntry.finishLoading( getExecutionContext() );
			}

			loadingCollectionMap = null;
		}
	}

}
