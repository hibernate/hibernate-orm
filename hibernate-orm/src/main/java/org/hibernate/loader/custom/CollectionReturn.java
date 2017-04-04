/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
