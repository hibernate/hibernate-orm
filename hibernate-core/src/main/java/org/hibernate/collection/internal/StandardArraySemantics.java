/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * CollectionSemantics implementation for arrays
 *
 * @author Steve Ebersole
 */
public class StandardArraySemantics implements CollectionSemantics<Object[]> {
	/**
	 * Singleton access
	 */
	public static final StandardArraySemantics INSTANCE = new StandardArraySemantics();

	private StandardArraySemantics() {
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.ARRAY;
	}

	@Override
	public Object[] instantiateRaw(
			int anticipatedSize,
			CollectionPersister collectionDescriptor) {
//		return (Object[]) Array.newInstance(
//				collectionDescriptor.getJavaTypeDescriptor().getJavaType().getComponentType(),
//				anticipatedSize
//		);
		throw new UnsupportedOperationException();
	}


	@Override
	public PersistentCollection instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentArrayHolder( key, session, collectionDescriptor );
	}

	@Override
	public PersistentCollection wrap(
			Object rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new PersistentArrayHolder( session, collectionDescriptor, rawCollection );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> Iterator<E> getElementIterator(Object[] rawCollection) {
		return (Iterator<E>) Arrays.stream( rawCollection ).iterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitElements(Object[] array, Consumer action) {
		if ( array == null ) {
			return;
		}

		for ( Object element : array ) {
			action.accept( element );
		}

	}
}
