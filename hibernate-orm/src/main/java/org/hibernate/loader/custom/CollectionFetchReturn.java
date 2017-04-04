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
 * Specifically a fetch return that refers to a collection association.
 *
 * @author Steve Ebersole
 */
public class CollectionFetchReturn extends FetchReturn {
	private final CollectionAliases collectionAliases;
	private final EntityAliases elementEntityAliases;

	public CollectionFetchReturn(
			String alias,
			NonScalarReturn owner,
			String ownerProperty,
			CollectionAliases collectionAliases,
			EntityAliases elementEntityAliases,
			LockMode lockMode) {
		super( owner, ownerProperty, alias, lockMode );
		this.collectionAliases = collectionAliases;
		this.elementEntityAliases = elementEntityAliases;
	}

	public CollectionAliases getCollectionAliases() {
		return collectionAliases;
	}

	public EntityAliases getElementEntityAliases() {
		return elementEntityAliases;
	}
}
