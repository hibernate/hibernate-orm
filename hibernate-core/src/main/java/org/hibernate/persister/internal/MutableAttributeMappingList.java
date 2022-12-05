/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * This mutable representation of FetchableList is meant to
 * exist temporarily to assit migration to the new contract.
 * @deprecated  Please get rid of it: such collections should be immutable.
 */
@Deprecated
public final class MutableAttributeMappingList implements AttributeMappingsList {

	private final List list;

	public MutableAttributeMappingList(final int length) {
		this.list = new ArrayList<>( length );
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public AttributeMapping getAttributeMapping(int idx) {
		return asAttributeMapping( this.list.get( idx ) );
	}

	@Override
	public void forEachFetchable(Consumer<? super Fetchable> consumer) {
		for ( int i = 0; i < list.size(); i++ ) {
			consumer.accept( asFetchable( list.get( i ) ) );
		}
	}

	@Override
	public void forEachAttributeMapping(final Consumer<? super AttributeMapping> consumer) {
		for ( int i = 0; i < list.size(); i++ ) {
			consumer.accept( asAttributeMapping( list.get( i ) ) );
		}
	}

	@Override
	public Fetchable getFetchable(final int idx) {
		return asFetchable( this.list.get( idx ) );
	}

	@Override
	public Iterable<AttributeMapping> iterateAsAttributeMappings() {
		return new AttributeMappingIterable();
	}

	@Override
	public void forEachAttributeMapping(final IndexedConsumer<AttributeMapping> consumer) {
		for ( int i = 0; i < list.size(); i++ ) {
			consumer.accept( i, getAttributeMapping( i ) );
		}
	}

	@Override
	public void forEachFetchable(final IndexedConsumer<Fetchable> consumer) {
		for ( int i = 0; i < list.size(); i++ ) {
			consumer.accept( i, getFetchable( i ) );
		}
	}

	public void clear() {
		this.list.clear();
	}

	public void add(final AttributeMapping attributeMapping) {
		this.list.add( attributeMapping );
	}

	public SingularAttributeMapping getSingularAttributeMapping(int idx) {
		//TBD get rid of this cast - but it's low priority: apparently
		//not used much at all after bootstrap.
		return (SingularAttributeMapping) this.list.get( idx );
	}

	public void setAttributeMapping(int i, AttributeMapping attributeMapping) {
		this.list.add( i, attributeMapping );
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

	private final class AttributeMappingIterable implements Iterable<AttributeMapping> {
		@Override
		public Iterator<AttributeMapping> iterator() {
			return new AttributeMappingIterator();
		}
	}

	private final class AttributeMappingIterator implements Iterator<AttributeMapping> {

		private Iterator iter = MutableAttributeMappingList.this.list.iterator();

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public AttributeMapping next() {
			return asAttributeMapping( iter.next() );
		}

	}

}
