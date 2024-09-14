/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.internal;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hibernate.collection.spi.AbstractMapSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentSortedMap;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardSortedMapSemantics<K,V> extends AbstractMapSemantics<SortedMap<K,V>,K,V> {
	/**
	 * Singleton access
	 */
	public static final StandardSortedMapSemantics<?,?> INSTANCE = new StandardSortedMapSemantics<>();

	private StandardSortedMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_MAP;
	}

	@Override
	public Class<SortedMap> getCollectionJavaType() {
		return SortedMap.class;
	}

	@Override
	public TreeMap<K,V> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return new TreeMap<K,V>(
				collectionDescriptor == null ? null : (Comparator) collectionDescriptor.getSortingComparator()
		);
	}

	@Override
	public PersistentCollection<V> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		//noinspection unchecked
		return new PersistentSortedMap<>( session, (Comparator<K>) collectionDescriptor.getSortingComparator() );
	}

	@Override
	public PersistentCollection<V> wrap(
			SortedMap<K,V> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSortedMap<>( session, rawCollection );
	}
}
