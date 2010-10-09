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
package org.hibernate.hql.ast.tree;

import java.util.ArrayList;

import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ASTPrinter;

import antlr.collections.AST;

/**
 * Common behavior - a node that contains a list of select expressions.
 *
 * @author josh
 */
public abstract class SelectExpressionList extends HqlSqlWalkerNode {
	/**
	 * Returns an array of SelectExpressions gathered from the children of the given parent AST node.
	 *
	 * @return an array of SelectExpressions gathered from the children of the given parent AST node.
	 */
	public SelectExpression[] collectSelectExpressions() {
		// Get the first child to be considered.  Sub-classes may do this differently in order to skip nodes that
		// are not select expressions (e.g. DISTINCT).
		AST firstChild = getFirstSelectExpression();
		AST parent = this;
		ArrayList list = new ArrayList( parent.getNumberOfChildren() );
		for ( AST n = firstChild; n != null; n = n.getNextSibling() ) {
			if ( n instanceof SelectExpression ) {
				list.add( n );
			}
			else {
				throw new IllegalStateException( "Unexpected AST: " + n.getClass().getName() + " " + new ASTPrinter( SqlTokenTypes.class ).showAsString( n, "" ) );
			}
		}
		return ( SelectExpression[] ) list.toArray( new SelectExpression[list.size()] );
	}

	/**
	 * Returns the first select expression node that should be considered when building the array of select
	 * expressions.
	 *
	 * @return the first select expression node that should be considered when building the array of select
	 *         expressions
	 */
	protected abstract AST getFirstSelectExpression();

}
