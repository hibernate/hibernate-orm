/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Helper for producing plain Java collection values for collection-valued query results.
 *
 * @author Gavin King
 */
public final class DetachedCollectionHelper {
	private DetachedCollectionHelper() {
	}

	public static Object loadAndCopy(
			PluralAttributeMapping attributeMapping,
			Object collectionKey,
			SharedSessionContractImplementor session,
			CollectionPart.Nature selectedPartNature) {
		if ( collectionKey == null ) {
			return null;
		}

		final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();
		final Object collection = collectionDescriptor.getCollectionType()
				.getCollection( collectionKey, session, null, Boolean.TRUE );
		if ( collection instanceof PersistentCollection<?> persistentCollection ) {
			session.initializeCollection( persistentCollection, false );
		}
		return copy( attributeMapping, collection, selectedPartNature );
	}

	public static Object copy(
			PluralAttributeMapping attributeMapping,
			Object collection,
			CollectionPart.Nature selectedPartNature) {
		if ( collection == null ) {
			return null;
		}
		if ( selectedPartNature != null ) {
			return copyPart( attributeMapping, collection, selectedPartNature );
		}
		if ( collection instanceof Map<?, ?> map ) {
			final Map<Object, Object> copy = instantiateMap( attributeMapping, map.size() );
			copy.putAll( map );
			return copy;
		}
		if ( collection instanceof Collection<?> collectionValue ) {
			final Collection<Object> copy = instantiateCollection( attributeMapping, collectionValue.size() );
			copy.addAll( collectionValue );
			return copy;
		}
		if ( collection.getClass().isArray() ) {
			final int length = Array.getLength( collection );
			final Object copy = Array.newInstance( collection.getClass().getComponentType(), length );
			System.arraycopy( collection, 0, copy, 0, length );
			return copy;
		}
		return collection;
	}

	private static LinkedHashSet<Object> copyPart(
			PluralAttributeMapping attributeMapping,
			Object collection,
			CollectionPart.Nature selectedPartNature) {
		final LinkedHashSet<Object> copy = new LinkedHashSet<>();
		if ( selectedPartNature == CollectionPart.Nature.INDEX ) {
			if ( collection instanceof Map<?, ?> map ) {
				copy.addAll( map.keySet() );
			}
			else {
				final int size = collectionSize( collection );
				final int listIndexBase = attributeMapping.getIndexMetadata().getListIndexBase();
				for ( int i = 0; i < size; i++ ) {
					copy.add( listIndexBase + i );
				}
			}
		}
		else if ( collection instanceof Map<?, ?> map ) {
			copy.addAll( map.values() );
		}
		else if ( collection instanceof Collection<?> collectionValue ) {
			copy.addAll( collectionValue );
		}
		else if ( collection.getClass().isArray() ) {
			final int length = Array.getLength( collection );
			for ( int i = 0; i < length; i++ ) {
				copy.add( Array.get( collection, i ) );
			}
		}
		return copy;
	}

	@SuppressWarnings("unchecked")
	private static Map<Object, Object> instantiateMap(PluralAttributeMapping attributeMapping, int size) {
		final Object rawCollection = attributeMapping.getCollectionDescriptor()
				.getCollectionSemantics()
				.instantiateRaw( size, attributeMapping.getCollectionDescriptor() );
		return rawCollection instanceof Map<?, ?>
				? (Map<Object, Object>) rawCollection
				: new LinkedHashMap<>( size );
	}

	@SuppressWarnings("unchecked")
	private static Collection<Object> instantiateCollection(PluralAttributeMapping attributeMapping, int size) {
		final Object rawCollection = attributeMapping.getCollectionDescriptor()
				.getCollectionSemantics()
				.instantiateRaw( size, attributeMapping.getCollectionDescriptor() );
		return rawCollection instanceof Collection<?>
				? (Collection<Object>) rawCollection
				: new LinkedHashSet<>( size );
	}

	private static int collectionSize(Object collection) {
		if ( collection instanceof Map<?, ?> map ) {
			return map.size();
		}
		if ( collection instanceof Collection<?> collectionValue ) {
			return collectionValue.size();
		}
		if ( collection.getClass().isArray() ) {
			return Array.getLength( collection );
		}
		return 0;
	}
}
