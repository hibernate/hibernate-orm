/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
import org.hibernate.hql.internal.ast.HqlSqlWalker;

/**
 * Common interface modeling the different HQL statements (i.e., INSERT, UPDATE, DELETE, SELECT).
 *
 * @author Steve Ebersole
 */
public interface Statement {

	/**
	 * Retrieve the "phase 2" walker which generated this statement tree.
	 *
	 * @return The HqlSqlWalker instance which generated this statement tree.
	 */
	public HqlSqlWalker getWalker();

	/**
	 * Return the main token type representing the type of this statement.
	 *
	 * @return The corresponding token type.
	 */
	public int getStatementType();

	/**
	 * Does this statement require the StatementExecutor?
	 * </p>
	 * Essentially, at the JDBC level, does this require an executeUpdate()?
	 *
	 * @return True if this statement should be handed off to the
	 * StatementExecutor to be executed; false otherwise.
	 */
	public boolean needsExecutor();
}
