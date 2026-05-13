/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;
import static org.hibernate.collection.spi.InitializerProducerBuilder.createArrayInitializerProducer;

/**
 * CollectionSemantics implementation for arrays
 *
 * @author Steve Ebersole
 */
@AllowReflection
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
	public <X> Object instantiateWithElements(
			int anticipatedSize,
			CollectionPersister collectionDescriptor,
			Collection<? extends X> elements) {
		final Class<?> elementClass =
				collectionDescriptor == null || collectionDescriptor.getElementClass() == null
						? Object.class
						: collectionDescriptor.getElementClass();
		final Object array = newInstance( elementClass, elements.size() );
		int i = 0;
		for ( X element : elements ) {
			set( array, i++, element );
		}
		return array;
	}

	@Override
	public int collectionSize(Object rawCollection) {
		return rawCollection != null && rawCollection.getClass().isArray() ? getLength( rawCollection ) : 0;
	}

	@Override
	public Object copy(
			Object rawCollection,
			CollectionPersister collectionDescriptor) {
		if ( rawCollection != null && rawCollection.getClass().isArray() ) {
			final int length = collectionSize( rawCollection );
			final Object copy = newInstance( rawCollection.getClass().getComponentType(), length );
			System.arraycopy( rawCollection, 0, copy, 0, length );
			return copy;
		}
		else {
			return rawCollection;
		}
	}

	@Override
	public Set<?> copyPart(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			CollectionPart.Nature partNature) {
		final LinkedHashSet<Object> copy = new LinkedHashSet<>();
		final int length = collectionSize( rawCollection );
		if ( partNature == CollectionPart.Nature.INDEX ) {
			final int listIndexBase =
					collectionDescriptor.getAttributeMapping()
							.getIndexMetadata().getListIndexBase();
			for ( int i = 0; i < length; i++ ) {
				copy.add( listIndexBase + i );
			}
		}
		else if ( rawCollection != null && rawCollection.getClass().isArray() ) {
			for ( int i = 0; i < length; i++ ) {
				copy.add( get( rawCollection, i ) );
			}
		}
		return copy;
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
		if ( array != null ) {
			for ( E element : array ) {
				action.accept( element );
			}
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
		return createArrayInitializerProducer(
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
