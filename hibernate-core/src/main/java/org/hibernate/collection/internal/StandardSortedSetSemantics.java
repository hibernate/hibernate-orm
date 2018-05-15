/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardSortedSetSemantics extends AbstractSetSemantics<SortedSet<?>> {
	/**
	 * Singleton access
	 */
	public static final StandardSortedSetSemantics INSTANCE = new StandardSortedSetSemantics();

	private StandardSortedSetSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_SET;
	}

	@Override
	public SortedSet instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return new TreeSet<>();
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentCollection instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedSet( session, collectionDescriptor, key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PersistentCollection wrap(
			SortedSet rawCollection,
			PersistentCollectionDescriptor collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedSet( session, collectionDescriptor, rawCollection );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Iterator<E> getElementIterator(SortedSet<?> rawCollection) {
		return (Iterator<E>) rawCollection.iterator();
	}
}
