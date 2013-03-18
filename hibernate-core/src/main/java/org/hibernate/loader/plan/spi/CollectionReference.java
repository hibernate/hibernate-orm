/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.LockMode;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Represents a reference to an owned collection either as a return or as a fetch
 *
 * @author Steve Ebersole
 */
public interface CollectionReference {
	/**
	 * Retrieve the alias associated with the persister (entity/collection).
	 *
	 * @return The alias
	 */
	public String getAlias();

	/**
	 * Retrieve the lock mode associated with this return.
	 *
	 * @return The lock mode.
	 */
	public LockMode getLockMode();

	/**
	 * Retrieves the CollectionPersister describing the collection associated with this Return.
	 *
	 * @return The CollectionPersister.
	 */
	public CollectionPersister getCollectionPersister();

	public FetchOwner getIndexGraph();

	public FetchOwner getElementGraph();

	public PropertyPath getPropertyPath();

	public boolean hasEntityElements();

	/**
	 * Returns the description of the aliases in the JDBC ResultSet that identify values "belonging" to the
	 * this collection.
	 *
	 * @return The ResultSet alias descriptor for the collection
	 */
	public CollectionAliases getCollectionAliases();

	/**
	 * If the elements of this collection are entities, this methods returns the JDBC ResultSet alias descriptions
	 * for that entity; {@code null} indicates a non-entity collection.
	 *
	 * @return The ResultSet alias descriptor for the collection's entity element, or {@code null}
	 */
	public EntityAliases getElementEntityAliases();
}
