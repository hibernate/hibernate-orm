/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection;

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

	@Override
	default boolean isCollectionInitializer() {
		return true;
	}

	@Override
	default CollectionInitializer<?> asCollectionInitializer() {
		return this;
	}
}
