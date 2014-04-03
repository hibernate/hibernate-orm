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

import org.hibernate.QueryException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Models what ANSI SQL terms a <tt>searched case expression</tt>.  This is a <tt>CASE</tt> expression
 * in the form<pre>
 * CASE
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SearchedCaseNode extends AbstractSelectExpression implements SelectExpression {
	@Override
	public Type getDataType() {
		// option is used to hold each WHEN/ELSE in turn
		AST option = getFirstChild();
		while ( option != null ) {
			final AST result;
			if ( option.getType() == HqlSqlTokenTypes.WHEN ) {
				result = option.getFirstChild().getNextSibling();
			}
			else if ( option.getType() == HqlSqlTokenTypes.ELSE ) {
				result = option.getFirstChild();
			}
			else {
				throw new QueryException(
						"Unexpected node type :" +
								ASTUtil.getTokenTypeName( HqlSqlTokenTypes.class, option.getType() ) +
								"; expecting WHEN or ELSE"
				);
			}

			if ( SqlNode.class.isInstance( result ) ) {
				final Type nodeDataType = ( (SqlNode) result ).getDataType();
				if ( nodeDataType != null ) {
					return nodeDataType;
				}
			}

			option = option.getNextSibling();
		}

		throw new QueryException( "Could not determine data type for searched case statement" );
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

}
