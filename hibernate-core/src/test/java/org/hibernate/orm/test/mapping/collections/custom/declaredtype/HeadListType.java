package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

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

/**
 * @author Steve Ebersole
 */
public class HeadListType implements UserCollectionType {

	@Override
	@Nonnull
	public CollectionClassification getClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	@Nonnull
	public Class<?> getCollectionClass() {
		return IHeadList.class;
	}

	@Override
	@Nonnull
	public PersistentCollection instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) throws HibernateException {
		return new PersistentHeadList( session );
	}

	@Override
	@Nonnull
	public PersistentCollection wrap(@Nonnull SharedSessionContractImplementor session, @Nonnull Object collection) {
		return new PersistentHeadList( session, (IHeadList) collection );
	}

	@Nonnull
	public Iterator getElementsIterator(@Nonnull Object collection) {
		return ( (IHeadList) collection ).iterator();
	}

	public boolean contains(@Nonnull Object collection, @Nullable Object entity) {
		return ( (IHeadList) collection ).contains( entity );
	}

	@Nullable
	public Object indexOf(@Nonnull Object collection, @Nonnull Object entity) {
		int l = ( (IHeadList) collection ).indexOf( entity );
		if ( l < 0 ) {
			return null;
		}
		else {
			return l;
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
		IHeadList result = (IHeadList) target;
		result.clear();
		result.addAll( (HeadList) original );
		return result;
	}

	@Nonnull
	public Object instantiate(int anticipatedSize) {
		return new HeadList();
	}


}
