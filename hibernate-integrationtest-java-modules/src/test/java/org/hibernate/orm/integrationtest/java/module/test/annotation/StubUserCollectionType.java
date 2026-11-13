package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nonnull
	public CollectionClassification getClassification() {
		return CollectionClassification.BAG;
	}

	@Override
	@Nonnull
	public Class<?> getCollectionClass() {
		return ArrayList.class;
	}

	@Override
	@Nonnull
	public PersistentCollection<?> instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public PersistentCollection<?> wrap(@Nonnull SharedSessionContractImplementor session, @Nonnull Object collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public Iterator<?> getElementsIterator(@Nonnull Object collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(@Nonnull Object collection, @Nonnull Object entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	public Object indexOf(@Nonnull Object collection, @Nonnull Object entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public Object replaceElements(
			@Nonnull Object original,
			@Nonnull Object target,
			@Nonnull CollectionPersister persister,
			@Nullable Object owner,
			@Nonnull Map copyCache,
			@Nonnull SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public Object instantiate(int anticipatedSize) {
		return new ArrayList<>();
	}
}
