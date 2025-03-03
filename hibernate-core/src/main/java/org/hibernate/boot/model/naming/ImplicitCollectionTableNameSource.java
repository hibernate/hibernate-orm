/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name for a collection table.
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.CollectionTable
 */
public interface ImplicitCollectionTableNameSource extends ImplicitNameSource {
	/**
	 * Access to the physical name of the owning entity's table.
	 *
	 * @return Owning entity's table name.
	 */
	Identifier getOwningPhysicalTableName();

	/**
	 * Access to entity naming information for the owning side.
	 *
	 * @return Owning entity naming information
	 */
	EntityNaming getOwningEntityNaming();

	/**
	 * Access to the name of the attribute, from the owning side, that defines the association.
	 *
	 * @return The owning side's attribute name.
	 */
	AttributePath getOwningAttributePath();
}
