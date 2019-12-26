/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;
import org.hibernate.LockMode;
import org.hibernate.loader.EntityAliases;

/**
 * Specifically a fetch return that refers to an entity association.
 *
 * @author Steve Ebersole
 */
public class EntityFetchReturn extends FetchReturn {
	private final EntityAliases entityAliases;

	public EntityFetchReturn(
			String alias,
			EntityAliases entityAliases,
			NonScalarReturn owner,
			String ownerProperty,
			LockMode lockMode) {
		super( owner, ownerProperty, alias, lockMode );
		this.entityAliases = entityAliases;
	}

	public EntityAliases getEntityAliases() {
		return entityAliases;
	}
}
