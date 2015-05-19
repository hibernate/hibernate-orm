/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.profile;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Models the association of a given fetch.
 *
 * @author Steve Ebersole
 */
public class Association {
	private final EntityPersister owner;
	private final String associationPath;
	private final String role;

	/**
	 * Constructs a association defining what is to be fetched.
	 *
	 * @param owner The entity owning the association
	 * @param associationPath The path of the association, from the entity
	 */
	public Association(EntityPersister owner, String associationPath) {
		this.owner = owner;
		this.associationPath = associationPath;
		this.role = owner.getEntityName() + '.' + associationPath;
	}

	public EntityPersister getOwner() {
		return owner;
	}

	public String getAssociationPath() {
		return associationPath;
	}

	public String getRole() {
		return role;
	}
}
