package org.hibernate.orm.test.mapping.collections.type;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

//tag::collections-custom-collection-mapping-example[]
public class QueueType implements UserCollectionType {
	@Override
	@Nonnull
	public CollectionClassification getClassification() {
		return CollectionClassification.BAG;
	}

	@Override
	@Nonnull
	public Class<?> getCollectionClass() {
		return Queue.class;
	}

	@Override
	@Nonnull
	public PersistentCollection instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) throws HibernateException {
		return new PersistentQueue(session);
	}

	@Override
	@Nonnull
	public PersistentCollection wrap(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object collection) {
		return new PersistentQueue(session, (List) collection);
	}

	@Override
	@Nonnull
	public Iterator getElementsIterator(@Nonnull Object collection) {
		return ((Queue) collection).iterator();
	}

	@Override
	public boolean contains(@Nonnull Object collection, @Nullable Object entity) {
		return ((Queue) collection).contains(entity);
	}

	@Override
	@Nullable
	public Object indexOf(@Nonnull Object collection, @Nonnull Object entity) {
		int i = ((List) collection).indexOf(entity);
		return (i < 0) ? null : i;
	}

	@Override
	@Nonnull
	public Object replaceElements(
			@Nonnull Object original,
			@Nonnull Object target,
			@Nonnull CollectionPersister persister,
			@Nullable Object owner,
			@Nonnull Map copyCache,
			@Nonnull SharedSessionContractImplementor session)
			throws HibernateException {
		Queue result = (Queue) target;
		result.clear();
		result.addAll((Queue) original);
		return result;
	}

	@Override
	@Nonnull
	public Object instantiate(int anticipatedSize) {
		return new LinkedList<>();
	}

}
//end::collections-custom-collection-mapping-example[]
