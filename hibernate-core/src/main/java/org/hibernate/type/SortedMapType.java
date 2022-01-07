/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;
import java.util.TreeMap;

import org.hibernate.collection.spi.PersistentSortedMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.spi.TypeConfiguration;


public class SortedMapType extends MapType {

	private final Comparator comparator;

	public SortedMapType(TypeConfiguration typeConfiguration, String role, String propertyRef, Comparator comparator) {
		super( typeConfiguration, role, propertyRef );
		this.comparator = comparator;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.SORTED_MAP;
	}

	public Class getReturnedClass() {
		return java.util.SortedMap.class;
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Object key) {
		return new PersistentSortedMap( session, comparator );
	}

	@SuppressWarnings( {"unchecked"})
	public Object instantiate(int anticipatedSize) {
		return new TreeMap(comparator);
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSortedMap( session, (java.util.SortedMap) collection );
	}
}
