/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name of a "join column" (think
 * {@link javax.persistence.JoinColumn}).
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.JoinColumn
 */
public interface ImplicitJoinColumnNameSource extends ImplicitNameSource {
	public static enum Nature {
		ELEMENT_COLLECTION,
		ENTITY_COLLECTION,
		ENTITY
	}

	public Nature getNature();

	/**
	 * Access to entity naming information.  For "normal" join columns, this will
	 * be the entity where the association is defined.  For "inverse" join columns,
	 * this will be the target entity.
	 *
	 * @return Owning entity naming information
	 */
	public EntityNaming getEntityNaming();

	/**
	 * Access to the name of the attribute that defines the association.  For
	 * "normal" join columns, this will be the attribute where the association is
	 * defined.  For "inverse" join columns, this will be the "mapped-by" attribute.
	 *
	 * @return The owning side's attribute name.
	 */
	public AttributePath getAttributePath();

	/**
	 * Access the name of the table that is the target of the FK being described
	 *
	 * @return The referenced table name
	 */
	public Identifier getReferencedTableName();

	/**
	 * Access the name of the column that is the target of the FK being described
	 *
	 * @return The referenced column name
	 */
	public Identifier getReferencedColumnName();
}
