/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.Set;

import org.hibernate.sql.ast.tree.spi.Statement;

/**
 * Base descriptor of SQL AST, including the tree ({link Statement})
 * as well as other information determined while building it.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface SqlAstDescriptor {
	/**
	 * The SQL AST tree
	 */
	Statement getSqlAstStatement();

	/**
	 * The tables that are affected by this query.  This
	 * is used to drive up-to-date checking on those tables
	 * to see if we have any pending changes against them and,
	 * if so, flush those changes.
	 */
	Set<String> getAffectedTableNames();
}
