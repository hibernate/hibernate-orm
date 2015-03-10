/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
