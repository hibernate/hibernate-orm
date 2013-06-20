/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.exec.spi;

import org.hibernate.loader.EntityAliases;

/**
 * Aggregates the alias/suffix information in relation to an {@link org.hibernate.loader.plan.spi.EntityReference}
 *
 * todo : add a contract (interface) that can be shared by entity and collection alias info objects as lhs/rhs of a join ?
 *
 * @author Steve Ebersole
 */
public interface EntityReferenceAliases {
	/**
	 * Obtain the table alias used for referencing the table of the EntityReference.
	 * <p/>
	 * Note that this currently just returns the "root alias" whereas sometimes an entity reference covers
	 * multiple tables.  todo : to help manage this, consider a solution like TableAliasRoot from the initial ANTLR re-work
	 * see http://anonsvn.jboss.org/repos/hibernate/core/branches/antlr3/src/main/java/org/hibernate/sql/ast/alias/TableAliasGenerator.java
	 *
	 * @return The (root) table alias for the described entity reference.
	 */
	public String getTableAlias();

	/**
	 * Obtain the column aliases for the select fragment columns associated with the described entity reference.  These
	 * are the column renames by which the values can be extracted from the SQL result set.
	 *
	 * @return The column aliases associated with the described entity reference.
	 */
	public EntityAliases getColumnAliases();
}
