/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Iterator;
import java.util.LinkedHashSet;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardOrderedSetSemantics extends AbstractSetSemantics<LinkedHashSet<?>> {
	/**
	 * Singleton access
	 */
	public static final StandardOrderedSetSemantics INSTANCE = new StandardOrderedSetSemantics();

	private StandardOrderedSetSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ORDERED_SET;
	}

	@Override
	public LinkedHashSet<?> instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		return anticipatedSize < 1 ? new LinkedHashSet() : new LinkedHashSet<>( anticipatedSize );
	}

	@Override
	public <E> PersistentCollection<E> instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor<?, LinkedHashSet<?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet( session, collectionDescriptor, key );
	}

	@Override
	public <E> PersistentCollection<E> wrap(
			LinkedHashSet<?> rawCollection,
			PersistentCollectionDescriptor<?, LinkedHashSet<?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentSet( session, collectionDescriptor, rawCollection );
	}

	@Override
	public Iterator getElementIterator(LinkedHashSet rawCollection) {
		return rawCollection.iterator();
	}
}
