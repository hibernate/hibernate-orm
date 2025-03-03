/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMapSemantics<MKV extends Map<K,V>, K, V> implements MapSemantics<MKV,K,V> {
	@Override
	public Class<? extends Map> getCollectionJavaType() {
		return Map.class;
	}

	@Override
	public Iterator<K> getKeyIterator(MKV rawMap) {
		if ( rawMap == null ) {
			return null;
		}

		return rawMap.keySet().iterator();
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
		if ( rawMap == null ) {
			return Collections.emptyIterator();
		}

		return rawMap.values().iterator();
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
		return InitializerProducerBuilder.createMapInitializerProducer(
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
