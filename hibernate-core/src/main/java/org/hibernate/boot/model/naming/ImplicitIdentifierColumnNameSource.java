/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name of an entity's identifier
 * column.
 *
 * @author Steve Ebersole
 */
public interface ImplicitIdentifierColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the entity name information
	 *
	 * @return The entity name information
	 */
	public EntityNaming getEntityNaming();

	/**
	 * Access to the AttributePath for the entity's identifier attribute.
	 *
	 * @return The AttributePath for the entity's identifier attribute.
	 */
	public AttributePath getIdentifierAttributePath();
}
