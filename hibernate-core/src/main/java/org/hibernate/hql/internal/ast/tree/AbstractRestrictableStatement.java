/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.internal.CoreMessageLogger;

import antlr.collections.AST;

/**
 * Convenience implementation of {@link RestrictableStatement}
 * to centralize common functionality.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractRestrictableStatement extends AbstractStatement implements RestrictableStatement {
	private FromClause fromClause;
	private AST whereClause;

	protected abstract int getWhereClauseParentTokenType();

	protected abstract CoreMessageLogger getLog();

	@Override
	public final FromClause getFromClause() {
		if ( fromClause == null ) {
			fromClause = (FromClause) ASTUtil.findTypeInChildren( this, HqlSqlTokenTypes.FROM );
		}
		return fromClause;
	}

	@Override
	public final boolean hasWhereClause() {
		AST whereClause = locateWhereClause();
		return whereClause != null && whereClause.getNumberOfChildren() > 0;
	}

	@Override
	public final AST getWhereClause() {
		if ( whereClause == null ) {
			whereClause = locateWhereClause();
			// If there is no WHERE node, make one.
			if ( whereClause == null ) {
				getLog().debug( "getWhereClause() : Creating a new WHERE clause..." );
				whereClause = getWalker().getASTFactory().create( HqlSqlTokenTypes.WHERE, "WHERE" );
				// inject the WHERE after the parent
				AST parent = ASTUtil.findTypeInChildren( this, getWhereClauseParentTokenType() );
				whereClause.setNextSibling( parent.getNextSibling() );
				parent.setNextSibling( whereClause );
			}
		}
		return whereClause;
	}

	protected AST locateWhereClause() {
		return ASTUtil.findTypeInChildren( this, HqlSqlTokenTypes.WHERE );
	}
}
