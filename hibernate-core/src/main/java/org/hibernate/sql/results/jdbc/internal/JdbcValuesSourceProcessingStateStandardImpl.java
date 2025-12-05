/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * @author Steve Ebersole
 */
public class JdbcValuesSourceProcessingStateStandardImpl implements JdbcValuesSourceProcessingState {
	private final JdbcValuesSourceProcessingOptions processingOptions;
	private final LoadedValuesCollector loadedValuesCollector;
	private final ExecutionContext executionContext;

	private List<EntityHolder> loadingEntityHolders;
	private List<EntityHolder> reloadedEntityHolders;
	private Map<CollectionKey, LoadingCollectionEntry> loadingCollectionMap;

	private final PreLoadEvent preLoadEvent;
	private final PostLoadEvent postLoadEvent;

	public JdbcValuesSourceProcessingStateStandardImpl(
			LoadedValuesCollector loadedValuesCollector,
			JdbcValuesSourceProcessingOptions processingOptions,
			ExecutionContext executionContext) {
		this.loadedValuesCollector = loadedValuesCollector;
		this.executionContext = executionContext;
		this.processingOptions = processingOptions;

		if ( executionContext.getSession().isEventSource() ) {
			final var eventSource = executionContext.getSession().asEventSource();
			preLoadEvent = new PreLoadEvent( eventSource );
			postLoadEvent = new PostLoadEvent( eventSource );
		}
		else {
			preLoadEvent = null;
			postLoadEvent = null;
		}
	}

	public JdbcValuesSourceProcessingStateStandardImpl(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions) {
		this( null, processingOptions, executionContext );
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
	public void registerLoadingEntityHolder(EntityHolder holder) {
		if ( loadingEntityHolders == null ) {
			loadingEntityHolders = new ArrayList<>();
		}
		loadingEntityHolders.add( holder );

		if ( loadedValuesCollector != null ) {
			loadedValuesCollector.registerEntity(
					holder.getEntityInitializer().getNavigablePath(),
					holder.getDescriptor(),
					holder.getEntityKey()
			);
		}
	}

	@Override
	public List<EntityHolder> getLoadingEntityHolders() {
		return loadingEntityHolders;
	}

	@Override
	public void registerReloadedEntityHolder(EntityHolder holder) {
		if ( reloadedEntityHolders == null ) {
			reloadedEntityHolders = new ArrayList<>();
		}
		reloadedEntityHolders.add( holder );
	}

	@Override
	public List<EntityHolder> getReloadedEntityHolders() {
		return reloadedEntityHolders;
	}

	@Override
	public LoadingCollectionEntry findLoadingCollectionLocally(CollectionKey key) {
		return loadingCollectionMap == null ? null : loadingCollectionMap.get( key );
	}

	@Override
	public void registerLoadingCollection(CollectionKey key, LoadingCollectionEntry loadingCollectionEntry) {
		if ( loadingCollectionMap == null ) {
			loadingCollectionMap = new HashMap<>();
		}
		loadingCollectionMap.put( key, loadingCollectionEntry );

		if ( loadedValuesCollector != null ) {
			loadedValuesCollector.registerCollection(
					loadingCollectionEntry.getInitializer().getNavigablePath(),
					loadingCollectionEntry.getCollectionDescriptor().getAttributeMapping(),
					key
			);
		}
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return executionContext.getSession();
	}

	@Override
	public void finishUp(boolean registerSubselects) {
		// now we can finalize loading collections
		finishLoadingCollections();
		getSession().getPersistenceContextInternal()
				.postLoad( this,
						registerSubselects ? executionContext::registerLoadingEntityHolder : null );
	}

	/**
	 * For Hibernate Reactive
	 */
	public void finishLoadingCollections() {
		if ( loadingCollectionMap != null ) {
			for ( var loadingCollectionEntry : loadingCollectionMap.values() ) {
				loadingCollectionEntry.finishLoading( getExecutionContext() );
			}
			loadingCollectionMap = null;
		}
	}

}
