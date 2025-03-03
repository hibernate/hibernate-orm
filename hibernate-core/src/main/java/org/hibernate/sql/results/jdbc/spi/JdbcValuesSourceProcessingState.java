/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.spi;

import java.util.List;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.sql.results.spi.LoadContexts;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Provides a context for processing the processing of the complete
 * set of rows from a JdbcValuesSource.  Holds in-flight state
 * and provides access to environmental information needed to perform the
 * processing.
 *
 * @author Steve Ebersole
 */
public interface JdbcValuesSourceProcessingState {
	ExecutionContext getExecutionContext();

	SharedSessionContractImplementor getSession();

	default QueryOptions getQueryOptions() {
		return getExecutionContext().getQueryOptions();
	}

	JdbcValuesSourceProcessingOptions getProcessingOptions();

	PreLoadEvent getPreLoadEvent();
	PostLoadEvent getPostLoadEvent();

	void registerLoadingEntityHolder(EntityHolder holder);

	List<EntityHolder> getLoadingEntityHolders();

	void registerReloadedEntityHolder(EntityHolder holder);

	List<EntityHolder> getReloadedEntityHolders();

	/**
	 * Find a LoadingCollectionEntry locally to this context.
	 *
	 * @see LoadContexts#findLoadingCollectionEntry(CollectionKey)
	 */
	LoadingCollectionEntry findLoadingCollectionLocally(CollectionKey key);

	/**
	 * Registers a LoadingCollectionEntry locally to this context
	 */
	void registerLoadingCollection(
			CollectionKey collectionKey,
			LoadingCollectionEntry loadingCollectionEntry);

	void finishUp(boolean registerSubselects);
}
