/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
