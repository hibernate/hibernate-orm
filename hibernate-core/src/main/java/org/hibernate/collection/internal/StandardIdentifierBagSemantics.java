/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Collection;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * CollectionSemantics implementation for id-bags
 *
 * @author Steve Ebersole
 */
public class StandardIdentifierBagSemantics extends AbstractBagSemantics<Collection<?>> {
	/**
	 * Singleton access
	 */
	public static final StandardIdentifierBagSemantics INSTANCE = new StandardIdentifierBagSemantics();

	private StandardIdentifierBagSemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.IDBAG;
	}

	@Override
	public <E> PersistentCollection<E> instantiateWrapper(
			Object key,
			PersistentCollectionDescriptor<?, Collection<?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentIdentifierBag<>( session, collectionDescriptor, key );
	}

	@Override
	public <E> PersistentCollection<E> wrap(
			Collection<?> rawCollection,
			PersistentCollectionDescriptor<?, Collection<?>, E> collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentIdentifierBag<>( session, collectionDescriptor, rawCollection );
	}
}
