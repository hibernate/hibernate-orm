/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

import static org.hibernate.collection.spi.InitializerProducerBuilder.createListInitializerProducer;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Hibernate's standard CollectionSemantics for Lists
 *
 * @author Steve Ebersole
 */
public class StandardListSemantics<E> implements CollectionSemantics<List<E>, E> {
	/**
	 * Singleton access
	 */
	public static final StandardListSemantics<?> INSTANCE = new StandardListSemantics<>();

	private StandardListSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.LIST;
	}

	@Override @SuppressWarnings("rawtypes")
	public Class<List> getCollectionJavaType() {
		return List.class;
	}

	@Override
	public List<E> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return arrayList( anticipatedSize );
	}

	@Override
	public <X> List<X> instantiateWithElements(
			int anticipatedSize,
			CollectionPersister collectionDescriptor,
			Collection<? extends X> elements) {
		final List<X> list = arrayList( anticipatedSize );
		list.addAll( elements );
		return list;
	}

	@Override
	public int collectionSize(Object rawCollection) {
		return rawCollection instanceof List<?> list ? list.size() : 0;
	}

	@Override
	public Object copy(
			Object rawCollection,
			CollectionPersister collectionDescriptor) {
		return rawCollection instanceof List<?> list
				? instantiateWithElements( collectionSize( rawCollection ), collectionDescriptor, list )
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
		else if ( rawCollection instanceof List<?> list ) {
			copy.addAll( list );
		}
		return copy;
	}

	@Override
	public Iterator<E> getElementIterator(List<E> rawCollection) {
		return rawCollection.iterator();
	}

	@Override
	public void visitElements(List<E> rawCollection, Consumer<? super E> action) {
		rawCollection.forEach( action );
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList<>( session );
	}

	@Override
	public PersistentCollection<E> wrap(
			List<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList<>( session, rawCollection );
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
		return createListInitializerProducer(
				navigablePath,
				attributeMapping,
				fetchParent,
				selected,
				indexFetch,
				elementFetch,
				creationState
		);
	}
}
