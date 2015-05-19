/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Represents a reference to a persistent collection either as a Return or as a {@link CollectionAttributeFetch}.
 *
 * @author Steve Ebersole
 */
public interface CollectionReference {
	/**
	 * Obtain the UID of the QuerySpace (specifically a {@link CollectionQuerySpace}) that this CollectionReference
	 * refers to.
	 *
	 * @return The UID
	 */
	public String getQuerySpaceUid();

	/**
	 * Retrieves the CollectionPersister describing the collection associated with this CollectionReference.
	 *
	 * @return The CollectionPersister.
	 */
	public CollectionPersister getCollectionPersister();

	/**
	 * Retrieve the metadata about the index of this collection *as a FetchSource*.  Will return
	 * {@code null} when:<ul>
	 *     <li>the collection is not indexed</li>
	 *     <li>the index is not a composite, entity, or "any" (cannot act as a FetchSource)</li>
	 * </ul>
	 * <p/>
	 * Works only for map keys, since a List index (int type) cannot act as a FetchSource.
	 * <p/>
	 *
	 * @return The collection index metadata as a FetchSource, or {@code null}.
	 */
	public CollectionFetchableIndex getIndexGraph();

	/**
	 * Retrieve the metadata about the elements of this collection *as a FetchSource*.  Will return
	 * {@code null} when the element is not a composite, entity, or "any" (cannot act as a FetchSource).
	 * Works only for map keys, since a List index cannot be anything other than an int which cannot be a FetchSource.
	 * <p/>
	 *
	 * @return The collection element metadata as a FetchSource, or {@code null}.
	 */
	public CollectionFetchableElement getElementGraph();

	/**
	 * Retrieve the PropertyPath to this reference.
	 *
	 * @return The PropertyPath
	 */
	public PropertyPath getPropertyPath();

	/**
	 * Should a collection element join be allowed? Returning <code>true</code>
	 * indicates that an element join can safely be added.
	 *
	 * @return true, if a collection index join is allowed.
	 */
	public boolean allowElementJoin();

	/**
	 * Should a collection index join be allowed? Returning <code>true</code>
	 * indicates that an index join can safely be added.
	 *
	 * @return true, if a collection index join is allowed.
	 */
	public boolean allowIndexJoin();
}
