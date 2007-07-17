package org.hibernate.loader.custom;

import org.hibernate.loader.EntityAliases;
import org.hibernate.LockMode;

/**
 * Spefically a fetch return that refers to an entity association.
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
