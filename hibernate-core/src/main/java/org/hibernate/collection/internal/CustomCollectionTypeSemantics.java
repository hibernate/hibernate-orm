/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.CollectionType;

import static org.hibernate.collection.spi.InitializerProducerBuilder.createCollectionTypeWrapperInitializerProducer;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMap;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedSet;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedSetOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.setOfSize;

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

	private static <X> @Nullable Comparator<X> getComparator(CollectionPersister collectionDescriptor) {
		//noinspection unchecked
		return collectionDescriptor == null ? null
				: (Comparator<X>) collectionDescriptor.getSortingComparator();
	}

	@Override
	public <X> Object instantiateWithElements(
			int anticipatedSize,
			CollectionPersister collectionDescriptor,
			Collection<? extends X> elements) {
		final Collection<X> collection =
				instantiateCollection( anticipatedSize, getCollectionClassification(), collectionDescriptor, elements.size() );
		collection.addAll( elements );
		return collection;
	}

	private static <X> Collection<X> instantiateCollection(
			int anticipatedSize,
			CollectionClassification collectionClassification,
			CollectionPersister collectionDescriptor,
			int size) {
		return switch ( collectionClassification ) {
			case ARRAY, LIST, BAG, ID_BAG -> CollectionHelper.arrayList( anticipatedSize );
			case SET -> anticipatedSize < 1 ? new HashSet<>() : setOfSize( anticipatedSize );
			case ORDERED_SET -> anticipatedSize < 1 ? linkedSet() : linkedSetOfSize( anticipatedSize );
			case SORTED_SET -> new TreeSet<>( getComparator( collectionDescriptor ) );
			case MAP, ORDERED_MAP, SORTED_MAP -> new LinkedHashSet<>( size );
		};
	}

	@Override
	public <K, V> Map<K, V> instantiateWithElements(
			int anticipatedSize,
			CollectionPersister collectionDescriptor,
			Map<? extends K, ? extends V> entries) {
		final Map<K, V> map = instantiateMap( anticipatedSize, getCollectionClassification(), collectionDescriptor );
		map.putAll( entries );
		return map;
	}

	private static <K, V> Map<K, V> instantiateMap(
			int anticipatedSize,
			CollectionClassification collectionClassification,
			CollectionPersister collectionDescriptor) {
		return switch ( collectionClassification ) {
			case ORDERED_MAP -> anticipatedSize < 1 ? linkedMap() : linkedMapOfSize( anticipatedSize );
			case SORTED_MAP -> new TreeMap<>( getComparator( collectionDescriptor ) );
			default -> anticipatedSize < 1 ? CollectionHelper.map() : mapOfSize( anticipatedSize );
		};
	}

	@Override
	public int collectionSize(Object rawCollection) {
		if ( rawCollection instanceof Collection<?> collection ) {
			return collection.size();
		}
		else if ( rawCollection instanceof Map<?, ?> map ) {
			return map.size();
		}
		else if ( rawCollection == null ) {
			return 0;
		}
		else {
			int size = 0;
			final var elements = collectionType.getElementsIterator( rawCollection );
			while ( elements.hasNext() ) {
				elements.next();
				size++;
			}
			return size;
		}
	}

	@Override
	public Object copy(
			Object rawCollection,
			CollectionPersister collectionDescriptor) {
		if ( rawCollection instanceof Collection<?> collection ) {
			return instantiateWithElements( collectionSize( rawCollection ), collectionDescriptor, collection );
		}
		else if ( rawCollection instanceof Map<?, ?> map ) {
			return instantiateWithElements( collectionSize( rawCollection ), collectionDescriptor, map );
		}
		else if ( rawCollection == null || getCollectionClassification().isMap() ) {
			return rawCollection;
		}
		else {
			final int size = collectionSize( rawCollection );
			final Collection<Object> copy =
					instantiateCollection( size, getCollectionClassification(), collectionDescriptor, size );
			final Iterator<?> elements = collectionType.getElementsIterator( rawCollection );
			while ( elements.hasNext() ) {
				copy.add( elements.next() );
			}
			return copy;
		}
	}

	@Override
	public Set<?> copyPart(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			CollectionPart.Nature partNature) {
		final LinkedHashSet<Object> copy = new LinkedHashSet<>();
		if ( partNature == CollectionPart.Nature.INDEX ) {
			if ( rawCollection instanceof Map<?, ?> map ) {
				copy.addAll( map.keySet() );
			}
			else {
				final int listIndexBase =
						collectionDescriptor.getAttributeMapping()
								.getIndexMetadata().getListIndexBase();
				for ( int i = 0; i < collectionSize( rawCollection ); i++ ) {
					copy.add( listIndexBase + i );
				}
			}
		}
		else if ( rawCollection instanceof Map<?, ?> map ) {
			copy.addAll( map.values() );
		}
		else if ( rawCollection instanceof Collection<?> collection ) {
			copy.addAll( collection );
		}
		else if ( rawCollection != null ) {
			final Iterator<?> elements = collectionType.getElementsIterator( rawCollection );
			while ( elements.hasNext() ) {
				copy.add( elements.next() );
			}
		}
		return copy;
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
		return createCollectionTypeWrapperInitializerProducer(
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
