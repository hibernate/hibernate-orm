package org.hibernate.orm.test.mapping.collections.custom.basic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

public class MyListType implements UserCollectionType {

	static int lastInstantiationRequest = -2;

	@Override
	@Nonnull
	public CollectionClassification getClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	@Nonnull
	public Class<?> getCollectionClass() {
		return IMyList.class;
	}

	@Override
	@Nonnull
	public PersistentCollection instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) throws HibernateException {
		return new PersistentMyList( session );
	}

	@Override
	@Nonnull
	public PersistentCollection wrap(@Nonnull SharedSessionContractImplementor session, @Nonnull Object collection) {
		return new PersistentMyList( session, (IMyList) collection );
	}

	@Override
	@Nonnull
	public Iterator getElementsIterator(@Nonnull Object collection) {
		return ( (IMyList) collection ).iterator();
	}

	@Override
	public boolean contains(@Nonnull Object collection, @Nonnull Object entity) {
		return ( (IMyList) collection ).contains(entity);
	}

	@Override
	@Nullable
	public Object indexOf(@Nonnull Object collection, @Nonnull Object entity) {
		int l = ( (IMyList) collection ).indexOf(entity);
		if(l<0) {
			return null;
		} else {
			return new Integer(l);
		}
	}

	@Override
	@Nonnull
	public Object replaceElements(
			@Nonnull Object original,
			@Nonnull Object target,
			@Nonnull CollectionPersister persister,
			@Nullable Object owner,
			@Nonnull Map copyCache,
			@Nonnull SharedSessionContractImplementor session) throws HibernateException {
		IMyList result = (IMyList) target;
		result.clear();
		result.addAll((MyList)original);
		return result;
	}

	@Override
	@Nonnull
	public Object instantiate(int anticipatedSize) {
		lastInstantiationRequest = anticipatedSize;
		return new MyList();
	}

}
