/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBagSemantics<B extends Collection<?>> implements CollectionSemantics<B> {
	@Override
	@SuppressWarnings("unchecked")
	public B instantiateRaw(
			int anticipatedSize,
			PersistentCollectionDescriptor collectionDescriptor) {
		if ( anticipatedSize < 1 ) {
			return (B) new ArrayList();
		}
		else {
			return (B) CollectionHelper.arrayList( anticipatedSize );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Iterator<E> getElementIterator(B rawCollection) {
		if ( rawCollection == null ) {
			return null;
		}

		return (Iterator<E>) rawCollection.iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(B rawCollection, Consumer action) {
		if ( rawCollection != null ) {
			rawCollection.forEach( action );
		}
	}
}
