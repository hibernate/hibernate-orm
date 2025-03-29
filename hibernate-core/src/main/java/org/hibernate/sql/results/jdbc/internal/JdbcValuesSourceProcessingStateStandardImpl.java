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
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

/**
 * @author Steve Ebersole
 */
public class JdbcValuesSourceProcessingStateStandardImpl implements JdbcValuesSourceProcessingState {

	private final ExecutionContext executionContext;
	private final JdbcValuesSourceProcessingOptions processingOptions;

	private List<EntityHolder> loadingEntityHolders;
	private List<EntityHolder> reloadedEntityHolders;
	private Map<CollectionKey, LoadingCollectionEntry> loadingCollectionMap;

	private final PreLoadEvent preLoadEvent;
	private final PostLoadEvent postLoadEvent;

	public JdbcValuesSourceProcessingStateStandardImpl(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions) {
		this.executionContext = executionContext;
		this.processingOptions = processingOptions;

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
	public void registerLoadingEntityHolder(EntityHolder holder) {
		if ( loadingEntityHolders == null ) {
			loadingEntityHolders = new ArrayList<>();
		}
		loadingEntityHolders.add( holder );
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

	private boolean isReadOnly() {
		if ( getQueryOptions().isReadOnly() != null ) {
			return getQueryOptions().isReadOnly();
		}
		else if ( getSession() instanceof EventSource ) {
			return getSession().isDefaultReadOnly();
		}
		else {
			return false;
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	public void finishLoadingCollections() {
		if ( loadingCollectionMap != null ) {
			for ( LoadingCollectionEntry loadingCollectionEntry : loadingCollectionMap.values() ) {
				loadingCollectionEntry.finishLoading( getExecutionContext() );
			}

			loadingCollectionMap = null;
		}
	}

}
