/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;

public final class ImmutableAttributeMappingsMap implements AttributeMappingsMap {

	//Intentionally avoid a Map<String,AttributeMapping> as
	//that would imply having a typecheck for AttributeMapping
	//on each read.
	//Since the array doesn't require a type check on reads,
	//we store the index into the array and retrieve from there
	//instead - an extra indirection but avoids type check
	//and related cache pollution issues.
	private final HashMap<String,Integer> mapStore;
	private final AttributeMapping[] orderedValues;

	public ImmutableAttributeMappingsMap(final LinkedHashMap<String,AttributeMapping> sortedSource) {
		final int size = sortedSource.size();
		orderedValues = new AttributeMapping[size];
		mapStore = new HashMap<>( size );
		int idx = 0;
		//populate both parallel representations
		for ( var entry : sortedSource.entrySet() ) {
			orderedValues[idx] = entry.getValue();
			mapStore.put( entry.getKey(), Integer.valueOf( idx ) );
			idx++;
		}
	}

	@Override
	public void forEachValue(final Consumer<? super AttributeMapping> action) {
		for ( var attributeMapping : orderedValues ) {
			action.accept( attributeMapping );
		}
	}

	public int size() {
		return orderedValues.length;
	}

	@Override
	public AttributeMapping get(final String name) {
		final Integer integer = mapStore.get( name );
		return integer == null ? null : orderedValues[integer];
	}

	@Override
	public Iterable<AttributeMapping> valueIterator() {
		return new AttributeMappingIterable();
	}

	private final class AttributeMappingIterable implements Iterable<AttributeMapping> {

		@Override
		public Iterator<AttributeMapping> iterator() {
			return new AttributeMappingIterator();
		}

	}

	private final class AttributeMappingIterator implements Iterator<AttributeMapping> {

		private int idx = 0;

		@Override
		public boolean hasNext() {
			return idx < ImmutableAttributeMappingsMap.this.orderedValues.length;
		}

		@Override
		public AttributeMapping next() {
			return ImmutableAttributeMappingsMap.this.orderedValues[ idx++ ];
		}

	}

}
