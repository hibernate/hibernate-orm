/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.loader.custom;

import org.hibernate.LockMode;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;

/**
 * Represents a return which names a collection role; it
 * is used in defining a custom query for loading an entity's
 * collection in non-fetching scenarios (i.e., loading the collection
 * itself as the "root" of the result).
 *
 * @author Steve Ebersole
 */
public class CollectionReturn extends NonScalarReturn {
	private final String ownerEntityName;
	private final String ownerProperty;
	private final CollectionAliases collectionAliases;
	private final EntityAliases elementEntityAliases;

	public CollectionReturn(
			String alias,
			String ownerEntityName,
			String ownerProperty,
			CollectionAliases collectionAliases,
	        EntityAliases elementEntityAliases,
			LockMode lockMode) {
		super( alias, lockMode );
		this.ownerEntityName = ownerEntityName;
		this.ownerProperty = ownerProperty;
		this.collectionAliases = collectionAliases;
		this.elementEntityAliases = elementEntityAliases;
	}

	/**
	 * Returns the class owning the collection.
	 *
	 * @return The class owning the collection.
	 */
	public String getOwnerEntityName() {
		return ownerEntityName;
	}

	/**
	 * Returns the name of the property representing the collection from the {@link #getOwnerEntityName}.
	 *
	 * @return The name of the property representing the collection on the owner class.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}

	public CollectionAliases getCollectionAliases() {
		return collectionAliases;
	}

	public EntityAliases getElementEntityAliases() {
		return elementEntityAliases;
	}
}
