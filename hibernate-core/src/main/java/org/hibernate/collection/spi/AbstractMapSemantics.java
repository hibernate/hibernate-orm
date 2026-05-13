/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

import static java.util.Collections.emptyIterator;
import static org.hibernate.collection.spi.InitializerProducerBuilder.createMapInitializerProducer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMapSemantics<MKV extends Map<K,V>, K, V> implements MapSemantics<MKV,K,V> {
	@Override
	public <X> Collection<X> instantiateWithElements(
			int anticipatedSize,
			CollectionPersister collectionDescriptor,
			Collection<? extends X> elements) {
		return new LinkedHashSet<>( elements );
	}

	@Override
	public int collectionSize(Object rawCollection) {
		return rawCollection instanceof Map<?, ?> map ? map.size() : 0;
	}

	@Override
	public Object copy(
			Object rawCollection,
			CollectionPersister collectionDescriptor) {
		return rawCollection instanceof Map<?, ?> map
				? instantiateWithElements( collectionSize( rawCollection ), collectionDescriptor, map )
				: rawCollection;
	}

	@Override
	public Set<?> copyPart(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			CollectionPart.Nature partNature) {
		final LinkedHashSet<Object> copy = new LinkedHashSet<>();
		if ( rawCollection instanceof Map<?, ?> map ) {
			copy.addAll( partNature == CollectionPart.Nature.INDEX ? map.keySet() : map.values() );
		}
		return copy;
	}

	@Override @SuppressWarnings("rawtypes")
	public Class<? extends Map> getCollectionJavaType() {
		return Map.class;
	}

	@Override
	public Iterator<K> getKeyIterator(MKV rawMap) {
		return rawMap == null ? null : rawMap.keySet().iterator();

	}

	@Override
	public void visitKeys(MKV rawMap, Consumer<? super K> action) {
		if ( rawMap != null ) {
			rawMap.keySet().forEach( action );
		}
	}

	@Override
	public void visitEntries(MKV rawMap, BiConsumer<? super K, ? super V> action) {
		if ( rawMap != null ) {
			rawMap.forEach( action );
		}
	}


	@Override
	public Iterator<V> getElementIterator(MKV rawMap) {
		return rawMap == null ? emptyIterator() : rawMap.values().iterator();

	}

	@Override
	public void visitElements(MKV rawMap, Consumer<? super V> action) {
		if ( rawMap != null ) {
			rawMap.values().forEach( action );
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
		return createMapInitializerProducer(
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
