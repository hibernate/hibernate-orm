/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.profile;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Identifies the association referenced by a {@link Fetch}.
 *
 * @author Steve Ebersole
 */
public class Association {
	private final EntityPersister owner;
	private final String associationPath;
	private final String role;

	/**
	 * Constructs an association defining what is to be fetched.
	 *
	 * @param owner The entity owning the association
	 * @param associationPath The path of the association, from the entity
	 */
	public Association(EntityPersister owner, String associationPath) {
		this.owner = owner;
		this.associationPath = associationPath;
		this.role = owner.getEntityName() + '.' + associationPath;
	}

	/**
	 * The persister of the owning entity.
	 */
	public EntityPersister getOwner() {
		return owner;
	}

	/**
	 * The property path
	 */
	public String getAssociationPath() {
		return associationPath;
	}

	/**
	 * The fully qualified role name
	 */
	public String getRole() {
		return role;
	}

	@Override
	public String toString() {
		return "Association[" + role + "]";
	}
}
