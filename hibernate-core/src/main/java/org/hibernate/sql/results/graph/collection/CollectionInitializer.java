/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection;

import jakarta.persistence.CacheStoreMode;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Initializer implementation for initializing collections (plural attributes)
 *
 * @author Steve Ebersole
 */
public interface CollectionInitializer<Data extends InitializerData> extends InitializerParent<Data> {
	@Override
	PluralAttributeMapping getInitializedPart();

	default CollectionPersister getInitializingCollectionDescriptor() {
		return getInitializedPart().getCollectionDescriptor();
	}

	@Nullable PersistentCollection<?> getCollectionInstance(Data data);

	default @Nullable PersistentCollection<?> getCollectionInstance(RowProcessingState rowProcessingState) {
		return getCollectionInstance( getData( rowProcessingState ) );
	}

	default @Nullable CacheStoreMode getCacheStoreMode() {
		return null;
	}

	@Override
	default boolean isCollectionInitializer() {
		return true;
	}

	@Override
	default CollectionInitializer<?> asCollectionInitializer() {
		return this;
	}
}
