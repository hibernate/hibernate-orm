/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

import org.hibernate.persister.collection.spi.CollectionPersister;

/**
 * Represents a reference to a persistent collection either as a Return or as a {@link FetchCollectionAttribute}.
 *
 * @author Steve Ebersole
 */
public interface CollectionReference {
	/**
	 * Retrieves the CollectionPersister describing the collection associated with this CollectionReference.
	 *
	 * @return The CollectionPersister.
	 */
	CollectionPersister getCollectionPersister();

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
	FetchableCollectionIndex getIndexGraph();

	/**
	 * Retrieve the metadata about the elements of this collection *as a FetchSource*.  Will return
	 * {@code null} when the element is not a composite, entity, or "any" (cannot act as a FetchSource).
	 * Works only for map keys, since a List index cannot be anything other than an int which cannot be a FetchSource.
	 * <p/>
	 *
	 * @return The collection element metadata as a FetchSource, or {@code null}.
	 */
	FetchableCollectionElement getElementGraph();

	/**
	 * Should a collection element join be allowed? Returning <code>true</code>
	 * indicates that an element join can safely be added.
	 *
	 * @todo back on ORM upstream this only returns true for entity elements.  we should consider supporting embeddables as well
	 *
	 * @return true, if a collection index join is allowed.
	 */
	boolean allowElementJoin();

	/**
	 * Should a collection index join be allowed? Returning <code>true</code>
	 * indicates that an index join can safely be added.
	 *
	 * @todo back on ORM upstream this only returns true for a number of truths, one of which is whether the index is entity type.  we should consider supporting embeddables as well

	 * @return true, if a collection index join is allowed.
	 */
	boolean allowIndexJoin();
}
