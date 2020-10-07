/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public class StandardOrderedMapSemantics extends AbstractMapSemantics<LinkedHashMap<Object, Object>> {
	/**
	 * Singleton access
	 */
	public static final StandardOrderedMapSemantics INSTANCE = new StandardOrderedMapSemantics();

	private StandardOrderedMapSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_MAP;
	}

	@Override
	public LinkedHashMap<Object, Object> instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
		return anticipatedSize < 1 ? CollectionHelper.linkedMap() : CollectionHelper.linkedMapOfSize( anticipatedSize );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap( session );
	}

	@Override
	public PersistentCollection wrap(
			LinkedHashMap<Object, Object> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentMap( session, rawCollection );
	}

	@Override
	public Iterator<Object> getElementIterator(LinkedHashMap<Object, Object> rawCollection) {
		return rawCollection.values().iterator();
	}
}
