/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityReference extends FetchSource {

	/**
	 * Obtain the UID of the QuerySpace (specifically a {@link EntityQuerySpace}) that this EntityReference
	 * refers to.
	 *
	 * @return The UID
	 */
	public String getQuerySpaceUid();

	/**
	 * Retrieves the EntityPersister describing the entity associated with this Return.
	 *
	 * @return The EntityPersister.
	 */
	public EntityPersister getEntityPersister();

	/**
	 * Get the description of this entity's identifier descriptor.
	 *
	 * @return The identifier description.
	 */
	public EntityIdentifierDescription getIdentifierDescription();
}
