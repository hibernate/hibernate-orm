// $Id: Statement.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import org.hibernate.hql.ast.HqlSqlWalker;

/**
 * Common interface modeling the different HQL statements (i.e., INSERT, UPDATE, DELETE, SELECT).
 *
 * @author Steve Ebersole
 */
public interface Statement {

	/**
	 * Retreive the "phase 2" walker which generated this statement tree.
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
