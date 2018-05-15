/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Hibernate's standard CollectionSemantics for Lists
 *
 * @author Steve Ebersole
 */
public class StandardListSemantics implements CollectionSemantics<List> {
	/**
	 * Singleton access
	 */
	public static final StandardListSemantics INSTANCE = new StandardListSemantics();

	private StandardListSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	public List instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return CollectionHelper.arrayList( anticipatedSize );
	}

	@Override
	public Iterator getElementIterator(List rawCollection) {
		return rawCollection.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(List rawCollection, Consumer action) {
		rawCollection.forEach( action );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentList( session, collectionDescriptor, key );
	}

	@Override
	public <E> PersistentCollection<E> wrap(
			List rawCollection,
			PersistentCollectionDescriptor<?, List, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return null;
	}
}
