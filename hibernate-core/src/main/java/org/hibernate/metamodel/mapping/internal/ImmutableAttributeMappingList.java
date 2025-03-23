/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;

public final class ImmutableAttributeMappingList implements AttributeMappingsList {

	//Not using a generic collection storage as that would imply type checks
	//on each access: it has been shown that iteration on AttributeMappingsList
	//collections was severely triggering https://bugs.openjdk.org/browse/JDK-8180450 .
	//Arrays are safe for types reads; not for writes! but we're only writing
	//during initialization.
	private final AttributeMapping[] list;

	private ImmutableAttributeMappingList(final ArrayList<AttributeMapping> objects) {
		this.list = objects.toArray( new AttributeMapping[0] );
	}

	@Override
	public int size() {
		return list.length;
	}

	@Override
	public AttributeMapping get(final int i) {
		return list[i]; //intentional unguarded array access: let it explode
	}

	@Override
	public void forEach(Consumer<? super AttributeMapping> attributeMappingConsumer) {
		for ( AttributeMapping o : list ) {
			attributeMappingConsumer.accept( o );
		}
	}

	@Override
	public void indexedForEach(final IndexedConsumer<? super AttributeMapping> consumer) {
		for ( int i = 0; i < list.length; i++ ) {
			consumer.accept( i, list[i] );
		}
	}

	//Intentionally not implementing Iterator
	public final class AttributeMappingIterator {

		private int idx = 0;

		public boolean hasNext() {
			return idx < ImmutableAttributeMappingList.this.list.length;
		}

		public AttributeMapping next() {
			return ImmutableAttributeMappingList.this.list[idx++];
		}

	}

	public static final class Builder {

		private final ArrayList<AttributeMapping> builderList;

		public Builder(final int sizeHint) {
			this.builderList = new ArrayList<>( sizeHint );
		}

		public void add(final AttributeMapping attributeMapping) {
			Objects.requireNonNull( attributeMapping );
			this.builderList.add( attributeMapping );
		}

		public AttributeMappingsList build() {
			return new ImmutableAttributeMappingList( builderList );
		}

		public boolean assertFetchableIndexes() {
			for ( int i = 0; i < builderList.size(); i++ ) {
				final AttributeMapping attributeMapping = builderList.get( i );
				assert i == attributeMapping.getFetchableKey();
			}
			return true;
		}
	}

}
