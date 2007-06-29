// $Id: RestrictableStatement.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import antlr.collections.AST;

/**
 * Type definition for Statements which are restrictable via a where-clause (and
 * thus also having a from-clause).
 *
 * @author Steve Ebersole
 */
public interface RestrictableStatement extends Statement {
	/**
	 * Retreives the from-clause in effect for this statement.
	 *
	 * @return The from-clause for this statement; could be null if the from-clause
	 * has not yet been parsed/generated.
	 */
	public FromClause getFromClause();

	/**
	 * Does this statement tree currently contain a where clause?
	 *
	 * @return True if a where-clause is found in the statement tree and
	 * that where clause actually defines restrictions; false otherwise.
	 */
	public boolean hasWhereClause();

	/**
	 * Retreives the where-clause defining the restriction(s) in effect for
	 * this statement.
	 * <p/>
	 * Note that this will generate a where-clause if one was not found, so caution
	 * needs to taken prior to calling this that restrictions will actually exist
	 * in the resulting statement tree (otherwise "unexpected end of subtree" errors
	 * might occur during rendering).
	 *
	 * @return The where clause.
	 */
	public AST getWhereClause();
}
