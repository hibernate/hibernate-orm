/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
}
