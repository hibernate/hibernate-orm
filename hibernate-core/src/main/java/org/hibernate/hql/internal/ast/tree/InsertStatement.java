/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;

/**
 * Defines a top-level AST node representing an HQL "insert select" statement.
 *
 * @author Steve Ebersole
 */
public class InsertStatement extends AbstractStatement {

	@Override
	public int getStatementType() {
		return HqlSqlTokenTypes.INSERT;
	}

	@Override
	public boolean needsExecutor() {
		return true;
	}

	/**
	 * Performs detailed semantic validation on this insert statement tree.
	 *
	 * @throws QueryException Indicates validation failure.
	 */
	public void validate() throws QueryException {
		getIntoClause().validateTypes( getSelectClause() );
	}

	/**
	 * Retrieve this insert statement's into-clause.
	 *
	 * @return The into-clause
	 */
	public IntoClause getIntoClause() {
		return (IntoClause) getFirstChild();
	}

	/**
	 * Retrieve this insert statement's select-clause.
	 *
	 * @return The select-clause.
	 */
	public SelectClause getSelectClause() {
		return ( (QueryNode) getIntoClause().getNextSibling() ).getSelectClause();
	}

}
