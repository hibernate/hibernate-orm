/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name of a "join column" (think
 * {@link jakarta.persistence.JoinColumn}).
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.JoinColumn
 */
public interface ImplicitJoinColumnNameSource extends ImplicitNameSource {
	enum Nature {
		ELEMENT_COLLECTION,
		ENTITY_COLLECTION,
		ENTITY
	}

	Nature getNature();

	/**
	 * Access to entity naming information.  For "normal" join columns, this will
	 * be the entity where the association is defined.  For "inverse" join columns,
	 * this will be the target entity.
	 *
	 * @return Owning entity naming information
	 */
	EntityNaming getEntityNaming();

	/**
	 * Access to the name of the attribute that defines the association.  For
	 * "normal" join columns, this will be the attribute where the association is
	 * defined.  For "inverse" join columns, this will be the "mapped-by" attribute.
	 *
	 * @return The owning side's attribute name.
	 */
	AttributePath getAttributePath();

	/**
	 * Access the name of the table that is the target of the FK being described
	 *
	 * @return The referenced table name
	 */
	Identifier getReferencedTableName();

	/**
	 * Access the name of the column that is the target of the FK being described
	 *
	 * @return The referenced column name
	 */
	Identifier getReferencedColumnName();
}
