/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.hql.internal.ast.tree;

/**
 * @author Jan-Willem Gmelig Meyling
 */
public interface TableReferenceNode {

	/**
	 * Returns table names which are referenced by this node. If the tables
	 * can not be determined it returns null.
	 *
	 * @return table names or null.
	 */
	public String[] getReferencedTables();

}
