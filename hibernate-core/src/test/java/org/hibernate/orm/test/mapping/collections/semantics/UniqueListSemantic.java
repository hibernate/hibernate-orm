/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.semantics;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
//tag::collections-custom-semantics-ex[]
public class UniqueListSemantic<E> implements CollectionSemantics<List<E>, E> {

	@Override
	public CollectionClassification getCollectionClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	public Class<?> getCollectionJavaType() {
		return List.class;
	}

	@Override
	public List<E> instantiateRaw(int anticipatedSize, CollectionPersister collectionDescriptor) {
		return arrayList( anticipatedSize );
	}

	@Override
	public PersistentList<E> instantiateWrapper(
			Object key,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new UniqueListWrapper<>( session );
	}

	// ...
//end::collections-custom-semantics-ex[]

	@Override
	public PersistentList<E> wrap(
			List<E> rawCollection,
			CollectionPersister collectionDescriptor,
			SharedSessionContractImplementor session) {
		return new UniqueListWrapper<>( session, rawCollection );
	}

	@Override
	public Iterator<E> getElementIterator(List<E> rawCollection) {
		return rawCollection.iterator();
	}

	@Override
	public void visitElements(List<E> rawCollection, Consumer<? super E> action) {
		rawCollection.forEach( action );
	}
//tag::collections-custom-semantics-ex[]
}
//end::collections-custom-semantics-ex[]
