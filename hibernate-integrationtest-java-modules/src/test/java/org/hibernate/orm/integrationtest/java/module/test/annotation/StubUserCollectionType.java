/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class StubUserCollectionType implements UserCollectionType {
	@Override
	public CollectionClassification getClassification() {
		return CollectionClassification.BAG;
	}

	@Override
	public Class<?> getCollectionClass() {
		return ArrayList.class;
	}

	@Override
	public PersistentCollection<?> instantiate(SharedSessionContractImplementor session, CollectionPersister persister) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersistentCollection<?> wrap(SharedSessionContractImplementor session, Object collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<?> getElementsIterator(Object collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return new ArrayList<>();
	}
}
