/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.sql.results.graph.Fetchable;

public final class ImmutableAttributeMappingList implements AttributeMappingsList {

	//Not using generic storage as that would imply automatic type checks
	//on each and every access: it's preferrable to control the type checks and cast explicitly
	//as iteration of Attributes has been shown to be very performance sensitive.
	//Not least, it's nice to make this immutable.
	private final Object[] list;

	private ImmutableAttributeMappingList(final ArrayList<Object> objects) {
		this.list = objects.toArray( new Object[0] );
	}

	@Override
	public int size() {
		return list.length;
	}

	@Override
	public AttributeMapping getAttributeMapping(final int i) {
		return asAttributeMapping( list[i] ); //unguarded array access - let it explode as this is strictly internal
	}

	@Override
	public void forEachFetchable(final Consumer<? super Fetchable> fetchableConsumer) {
		for ( Object o : list ) {
			fetchableConsumer.accept( asFetchable( o ) );
		}
	}

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> attributeMappingConsumer) {
		for ( Object o : list ) {
			attributeMappingConsumer.accept( asAttributeMapping( o ) );
		}
	}

	@Override
	public Fetchable getFetchable(final int i) {
		return asFetchable( this.list[i] );
	}

	private static Fetchable asFetchable(final Object o) {
		//Type checking to Fetchable is expensive because of the impact of JDK-8180450;
		//we mitigate the problem by leveraging the pretty good bet that most of these will
		//in practice be represented by an BasicAttributeMapping
		if ( o instanceof BasicAttributeMapping ) {
			return (BasicAttributeMapping) o;
		}
		else {
			return (Fetchable) o;
		}
	}

	private static AttributeMapping asAttributeMapping(final Object o) {
		if ( o instanceof BasicAttributeMapping ) {
			return (BasicAttributeMapping) o;
		}
		else {
			return ((Fetchable) o).asAttributeMapping();
		}
	}

	@Override
	public Iterable<AttributeMapping> iterateAsAttributeMappings() {
		return new AttributeMappingIterable();
	}

	@Override
	public void forEachAttributeMapping(final IndexedConsumer<AttributeMapping> consumer) {
		for ( int i = 0; i < list.length; i++ ) {
			consumer.accept( i, asAttributeMapping( list[i] ) );
		}
	}

	@Override
	public void forEachFetchable(final IndexedConsumer<Fetchable> consumer) {
		for ( int i = 0; i < list.length; i++ ) {
			consumer.accept( i, asFetchable( list[i] ) );
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
			return idx < ImmutableAttributeMappingList.this.list.length;
		}

		@Override
		public AttributeMapping next() {
			return asAttributeMapping( ImmutableAttributeMappingList.this.list[idx++] );
		}

	}

	public static final class Builder {

		private final ArrayList<Object> builderList;

		public Builder(final int sizeHint) {
			this.builderList = new ArrayList<>( sizeHint );
		}

		public void add(final AttributeMapping attributeMapping) {
			this.builderList.add( attributeMapping );
		}

		public AttributeMappingsList build() {
			return new ImmutableAttributeMappingList( builderList );
		}

	}

}
