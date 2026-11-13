package org.hibernate.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A custom type for mapping user-written classes which implement {@link PersistentCollection}.
 *
 * @see PersistentCollection
 * @see org.hibernate.annotations.CollectionType#type()
 * @see org.hibernate.annotations.CollectionTypeRegistration#type()
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface UserCollectionType {
	/**
	 * The classification mapped by this custom type
	 */
	@Nonnull
	CollectionClassification getClassification();

	/**
	 * The Java type that this type maps.
	 */
	@Nonnull
	Class<?> getCollectionClass();

	/**
	 * Instantiate an uninitialized instance of the collection wrapper
	 */
	@Nonnull
	PersistentCollection<?> instantiate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull CollectionPersister persister)
			throws HibernateException;

	/**
	 * Wrap an instance of a collection
	 */
	@Nonnull
	PersistentCollection<?> wrap(@Nonnull SharedSessionContractImplementor session, @Nonnull Object collection);

	/**
	 * Return an iterator over the elements of this collection - the passed collection
	 * instance may or may not be a wrapper
	 */
	@Nonnull
	Iterator<?> getElementsIterator(@Nonnull Object collection);

	/**
	 * Optional operation. Does the collection contain the entity instance?
	 */
	boolean contains(@Nonnull Object collection, @Nonnull Object entity);

	/**
	 * Optional operation. Return the index of the entity in the collection.
	 */
	@Nullable
	Object indexOf(@Nonnull Object collection, @Nonnull Object entity);

	/**
	 * Replace the elements of a collection with the elements of another collection
	 */
	@Nonnull
	Object replaceElements(
			@Nonnull Object original,
			@Nonnull Object target,
			@Nonnull CollectionPersister persister,
			@Nullable Object owner,
			@Nonnull Map copyCache,
			@Nonnull SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper),
	 * but with the given anticipated size (i.e. accounting for initial size
	 * and perhaps load factor).
	 *
	 * @param anticipatedSize The anticipated size of the instantiated collection
	 * after we are done populating it.  Note, may be negative to indicate that
	 * we not yet know anything about the anticipated size (i.e., when initializing
	 * from a result set row by row).
	 */
	@Nonnull
	Object instantiate(int anticipatedSize);

}
