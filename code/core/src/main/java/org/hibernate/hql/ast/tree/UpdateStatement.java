// $Id: UpdateStatement.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ASTUtil;

import antlr.collections.AST;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a top-level AST node representing an HQL update statement.
 *
 * @author Steve Ebersole
 */
public class UpdateStatement extends AbstractRestrictableStatement {

	private static final Log log = LogFactory.getLog( UpdateStatement.class );

	/**
	 * @see org.hibernate.hql.ast.tree.Statement#getStatementType()
	 */
	public int getStatementType() {
		return SqlTokenTypes.UPDATE;
	}

	/**
	 * @see org.hibernate.hql.ast.tree.Statement#needsExecutor()
	 */
	public boolean needsExecutor() {
		return true;
	}

	protected int getWhereClauseParentTokenType() {
		return SqlTokenTypes.SET;
	}

	protected Log getLog() {
		return log;
	}

	public AST getSetClause() {
		return ASTUtil.findTypeInChildren( this, HqlSqlTokenTypes.SET );
	}
}
