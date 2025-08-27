/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.InitializerProducerBuilder;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * CollectionSemantics implementation for arrays
 *
 * @author Steve Ebersole
 */
public class StandardArraySemantics<E> implements CollectionSemantics<E[], E> {
	/**
	 * Singleton access
	 */
	public static final StandardArraySemantics<?> INSTANCE = new StandardArraySemantics<>();

	private StandardArraySemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ARRAY;
	}

	@Override
	public Class<Object[]> getCollectionJavaType() {
		return Object[].class;
	}

	@Override
	public E[] instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
//		return (Object[]) Array.newInstance(
//				collectionDescriptor.getJavaType().getJavaType().getComponentType(),
//				anticipatedSize
//		);
		throw new UnsupportedOperationException();
	}


	@Override
	public PersistentCollection<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentArrayHolder<>( session, collectionDescriptor );
	}

	@Override
	public PersistentCollection<E> wrap(
			E[] rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentArrayHolder<>( session, rawCollection );
	}

	@Override
	public Iterator<E> getElementIterator(E[] rawCollection) {
		return Arrays.stream( rawCollection ).iterator();
	}

	@Override
	public void visitElements(E[] array, Consumer<? super E> action) {
		if ( array == null ) {
			return;
		}

		for ( E element : array ) {
			action.accept( element );
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
		return InitializerProducerBuilder.createArrayInitializerProducer(
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
