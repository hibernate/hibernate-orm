/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.sql.results.graph.Fetchable;

public final class ImmutableAttributeMappingsMap implements AttributeMappingsMap {

	//Intentionally avoid generics and prefer raw storage
	//so that we can control how exactly types ar being casted
	private final HashMap mapStore;
	private final Object[] orderedValues;

	public ImmutableAttributeMappingsMap(final LinkedHashMap sortedSource) {
		this.orderedValues = sortedSource.values().toArray();
		this.mapStore = new HashMap( sortedSource );
	}

	@Override
	public void forEachValue(final Consumer<? super AttributeMapping> action) {
		for ( Object o : orderedValues ) {
			action.accept( asAttributeMapping( o ) );
		}
	}

	public int size() {
		return orderedValues.length;
	}

	@Override
	public AttributeMapping get(final String name) {
		final Object o = this.mapStore.get( name );
		if ( o == null ) {
			return null;
		}
		else {
			return asAttributeMapping( o );
		}
	}

	@Override
	public Iterable<AttributeMapping> valueIterator() {
		return new AttributeMappingIterable();
	}

	private static AttributeMapping asAttributeMapping(final Object o) {
		if ( o instanceof BasicAttributeMapping ) {
			return (BasicAttributeMapping) o;
		}
		else {
			AttributeMapping attributeMapping = ( (Fetchable) o ).asAttributeMapping();
			assert attributeMapping != null;
			return attributeMapping;
		}
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
			return asAttributeMapping( ImmutableAttributeMappingsMap.this.orderedValues[ idx++ ] );
		}

	}

}
