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

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardSortedSetSemantics extends AbstractSetSemantics<SortedSet<Object>> {
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
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public Class<SortedSet<Object>> getCollectionJavaType() {
		return (Class) SortedSet.class;
	}

	@Override
	public SortedSet<Object> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return new TreeSet<>( collectionDescriptor.getSortingComparator() );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedSet( session );
	}

	@Override
	public PersistentCollection wrap(
			SortedSet<Object> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedSet( session, rawCollection );
	}

	@Override
	public Iterator<Object> getElementIterator(SortedSet<Object> rawCollection) {
		return rawCollection.iterator();
	}
}
