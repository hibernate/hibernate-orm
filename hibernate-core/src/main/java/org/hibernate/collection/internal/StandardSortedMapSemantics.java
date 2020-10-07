/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardSortedMapSemantics extends AbstractMapSemantics<SortedMap<Object, Object>> {
	/**
	 * Singleton access
	 */
	public static final StandardSortedMapSemantics INSTANCE = new StandardSortedMapSemantics();

	private StandardSortedMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_MAP;
	}

	@Override
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public Class<SortedMap<Object, Object>> getCollectionJavaType() {
		return (Class) SortedMap.class;
	}

	@Override
	public SortedMap<Object, Object> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return new TreeMap<>( collectionDescriptor.getSortingComparator() );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedMap( session );
	}

	@Override
	public PersistentCollection wrap(
			SortedMap<Object, Object> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedMap( session, rawCollection );
	}
}
