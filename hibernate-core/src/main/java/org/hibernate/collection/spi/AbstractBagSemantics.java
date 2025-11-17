/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBagSemantics<E> implements BagSemantics<Collection<E>,E> {
	@Override
	public Class<Collection> getCollectionJavaType() {
		return Collection.class;
	}

	@Override
	public Collection<E> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		if ( anticipatedSize < 1 ) {
			return new ArrayList<>();
		}
		else {
			return CollectionHelper.arrayList( anticipatedSize );
		}
	}

	@Override
	public Iterator<E> getElementIterator(Collection<E> rawCollection) {
		if ( rawCollection == null ) {
			return null;
		}
		return rawCollection.iterator();
	}

	@Override
	public void visitElements(Collection<E> rawCollection, Consumer<? super E> action) {
		if ( rawCollection != null ) {
			rawCollection.forEach( action );
		}
	}

	@Override
	public CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			Fetch indexFetch,
			Fetch elementFetch,
			DomainResultCreationState creationState) {
		return InitializerProducerBuilder.createBagInitializerProducer(
				navigablePath,
				attributeMapping,
				fetchParent,
				selected,
				elementFetch,
				creationState
		);
	}

}
