/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.sql.exec.results.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.spi.InitializerSource;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityReference extends FetchParent, InitializerSource {
	/**
	 * Retrieves the entity persister describing the entity associated with this Return.
	 *
	 * @return The EntityPersister.
	 */
	EntityDescriptor getEntityDescriptor();

	/**
	 * Get the description of the entity's identifier, specific to this query
	 *
	 * @todo (6.0) - do we need this?
	 *
	 * @return The identifier description.
	 */
	EntityIdentifierReference getIdentifierReference();

	@Override
	EntityReferenceInitializer getInitializer();
}
