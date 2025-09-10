/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ops.spi.LoadedValuesCollector;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;

/**
 * @author Steve Ebersole
 */
public class ProposedProcessingState extends JdbcValuesSourceProcessingStateStandardImpl {
	private final LoadedValuesCollector loadedValuesCollector;

	public ProposedProcessingState(
			LoadedValuesCollector loadedValuesCollector,
			JdbcValuesSourceProcessingOptions processingOptions,
			ExecutionContext executionContext) {
		super( executionContext, processingOptions );
		this.loadedValuesCollector = loadedValuesCollector;
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		super.registerLoadingEntityHolder( holder );
		if ( loadedValuesCollector != null ) {
			loadedValuesCollector.registerEntity(
					holder.getEntityInitializer().getNavigablePath(),
					holder.getDescriptor(),
					holder.getEntityKey()
			);
		}
	}

	@Override
	public void registerLoadingCollection(CollectionKey key, LoadingCollectionEntry loadingCollectionEntry) {
		super.registerLoadingCollection( key, loadingCollectionEntry );
		if ( loadedValuesCollector != null ) {
			loadedValuesCollector.registerCollection(
					loadingCollectionEntry.getInitializer().getNavigablePath(),
					loadingCollectionEntry.getCollectionDescriptor().getAttributeMapping(),
					key
			);
		}
	}
}
