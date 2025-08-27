/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.InitializerProducerBuilder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.CollectionType;

/**
 * A collection semantics wrapper for <code>CollectionType</code>.
 *
 * @author Christian Beikov
 */
public class CustomCollectionTypeSemantics<CE, E> implements CollectionSemantics<CE, E> {
	private final CollectionType collectionType;

	public CustomCollectionTypeSemantics(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	public CollectionType getCollectionType() {
		return collectionType;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return collectionType.getCollectionClassification();
	}

	@Override
	public Class<?> getCollectionJavaType() {
		return collectionType.getReturnedClass();
	}

	@Override
	public CE instantiateRaw(int anticipatedSize, CollectionPersister collectionDescriptor) {
		//noinspection unchecked
		return (CE) collectionType.instantiate( anticipatedSize );
	}

	@Override
	public Iterator<E> getElementIterator(CE rawCollection) {
		//noinspection unchecked
		return (Iterator<E>) collectionType.getElementsIterator( rawCollection );
	}

	@Override
	public void visitElements(CE rawCollection, Consumer<? super E> action) {
		getElementIterator( rawCollection ).forEachRemaining( action );
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
		return InitializerProducerBuilder.createCollectionTypeWrapperInitializerProducer(
				navigablePath,
				attributeMapping,
				getCollectionClassification(),
				fetchParent,
				selected,
				indexFetch,
				elementFetch,
				creationState
		);
	}

	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		return (PersistentCollection<E>) collectionType.instantiate( session, collectionDescriptor, key );
	}

	@Override
	public PersistentCollection<E> wrap(
			CE rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		return (PersistentCollection<E>) collectionType.wrap( session, rawCollection );
	}
}
