/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.internal.ast.tree;
import antlr.collections.AST;

import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.internal.CoreMessageLogger;

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

	/**
	 * @see org.hibernate.hql.internal.ast.tree.RestrictableStatement#getFromClause
	 */
	public final FromClause getFromClause() {
		if ( fromClause == null ) {
			fromClause = ( FromClause ) ASTUtil.findTypeInChildren( this, HqlSqlTokenTypes.FROM );
		}
		return fromClause;
	}

	/**
	 * @see RestrictableStatement#hasWhereClause
	 */
	public final boolean hasWhereClause() {
		AST whereClause = locateWhereClause();
		return whereClause != null && whereClause.getNumberOfChildren() > 0;
	}

	/**
	 * @see org.hibernate.hql.internal.ast.tree.RestrictableStatement#getWhereClause
	 */
	public final AST getWhereClause() {
		if ( whereClause == null ) {
			whereClause = locateWhereClause();
			// If there is no WHERE node, make one.
			if ( whereClause == null ) {
				getLog().debug( "getWhereClause() : Creating a new WHERE clause..." );
				whereClause = ASTUtil.create( getWalker().getASTFactory(), HqlSqlTokenTypes.WHERE, "WHERE" );
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
