package org.hibernate.orm.test.mapping.collections.semantics;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

/**
 * @author Steve Ebersole
 */
//tag::collections-custom-type-ex[]
public class UniqueListType implements UserCollectionType {
	@Override
	@Nonnull
	public CollectionClassification getClassification() {
		return CollectionClassification.LIST;
	}

	@Override
	@Nonnull
	public Class<?> getCollectionClass() {
		return List.class;
	}

	@Override
	@Nonnull
	public PersistentCollection instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister) {
		return new UniqueListWrapper( session );
	}

	@Override
	@Nonnull
	public PersistentCollection wrap(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object collection) {
		return new UniqueListWrapper( session, (List) collection );
	}

	@Override
	@Nonnull
	public Iterator getElementsIterator(@Nonnull Object collection) {
		return ( (List) collection ).iterator();
	}

	@Override
	public boolean contains(@Nonnull Object collection, @Nonnull Object entity) {
		return ( (List) collection ).contains( entity );
	}

	@Override
	@Nullable
	public Object indexOf(@Nonnull Object collection, @Nonnull Object entity) {
		return ( (List) collection ).indexOf( entity );
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
		List result = (List) target;
		result.clear();
		result.addAll( (List) original );
		return result;
	}

	@Override
	@Nonnull
	public Object instantiate(int anticipatedSize) {
		return new ArrayList<>();
	}
}
//end::collections-custom-type-ex[]
