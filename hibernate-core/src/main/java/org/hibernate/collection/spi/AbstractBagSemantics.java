/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

import static org.hibernate.collection.spi.InitializerProducerBuilder.createBagInitializerProducer;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBagSemantics<E> implements BagSemantics<Collection<E>,E> {
	@Override @SuppressWarnings("rawtypes")
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
			return arrayList( anticipatedSize );
		}
	}

	@Override
	public <X> Collection<X> instantiateWithElements(
			int anticipatedSize,
			CollectionPersister collectionDescriptor,
			Collection<? extends X> elements) {
		final Collection<X> collection =
				anticipatedSize < 1
						? new ArrayList<>()
						: arrayList( anticipatedSize );
		collection.addAll( elements );
		return collection;
	}

	@Override
	public int collectionSize(Object rawCollection) {
		return rawCollection instanceof Collection<?> collection ? collection.size() : 0;
	}

	@Override
	public Object copy(
			Object rawCollection,
			CollectionPersister collectionDescriptor) {
		return rawCollection instanceof Collection<?> collection
				? instantiateWithElements( collectionSize( rawCollection ), collectionDescriptor, collection )
				: rawCollection;
	}

	@Override
	public Set<?> copyPart(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			CollectionPart.Nature partNature) {
		final LinkedHashSet<Object> copy = new LinkedHashSet<>();
		if ( partNature == CollectionPart.Nature.INDEX ) {
			final int listIndexBase =
					collectionDescriptor.getAttributeMapping()
							.getIndexMetadata().getListIndexBase();
			for ( int i = 0; i < collectionSize( rawCollection ); i++ ) {
				copy.add( listIndexBase + i );
			}
		}
		else if ( rawCollection instanceof Collection<?> collection ) {
			copy.addAll( collection );
		}
		return copy;
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
		return createBagInitializerProducer(
				navigablePath,
				attributeMapping,
				fetchParent,
				selected,
				elementFetch,
				creationState
		);
	}

}
