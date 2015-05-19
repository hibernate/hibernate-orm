/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name for a collection table.
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.CollectionTable
 */
public interface ImplicitCollectionTableNameSource extends ImplicitNameSource {
	/**
	  * Access to the physical name of the owning entity's table.
	  *
	  * @return Owning entity's table name.
	  */
	public Identifier getOwningPhysicalTableName();

	/**
	 * Access to entity naming information for the owning side.
	 *
	 * @return Owning entity naming information
	 */
	public EntityNaming getOwningEntityNaming();

	/**
	 * Access to the name of the attribute, from the owning side, that defines the association.
	 *
	 * @return The owning side's attribute name.
	 */
	public AttributePath getOwningAttributePath();
}
