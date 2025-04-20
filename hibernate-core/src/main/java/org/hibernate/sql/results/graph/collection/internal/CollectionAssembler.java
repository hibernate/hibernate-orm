/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.function.BiConsumer;

import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Andrea Boriero
 */
public class CollectionAssembler implements DomainResultAssembler {
	private final PluralAttributeMapping fetchedMapping;

	protected final CollectionInitializer<?> initializer;

	public CollectionAssembler(PluralAttributeMapping fetchedMapping, CollectionInitializer<?> initializer) {
		this.fetchedMapping = fetchedMapping;
		this.initializer = initializer;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState) {
		assert initializer.getData( rowProcessingState ).getState() != Initializer.State.UNINITIALIZED
				&& initializer.getData( rowProcessingState ).getState() != Initializer.State.KEY_RESOLVED;
		final PersistentCollection<?> collectionInstance = initializer.getCollectionInstance( rowProcessingState );
		return collectionInstance instanceof PersistentArrayHolder
				? collectionInstance.getValue()
				: collectionInstance;
	}

	@Override
	public JavaType<?> getAssembledJavaType() {
		return fetchedMapping.getJavaType();
	}

	@Override
	public CollectionInitializer<?> getInitializer() {
		return initializer;
	}

	@Override
	public void resolveState(RowProcessingState rowProcessingState) {
		initializer.resolveState( rowProcessingState );
	}

	@Override
	public void forEachResultAssembler(BiConsumer consumer, Object arg) {
		if ( initializer.isResultInitializer() ) {
			consumer.accept( initializer, arg );
		}
	}
}
