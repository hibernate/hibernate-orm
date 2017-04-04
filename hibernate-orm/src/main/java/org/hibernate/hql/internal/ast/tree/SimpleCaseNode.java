/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * Models what ANSI SQL terms a simple case statement.  This is a <tt>CASE</tt> expression in the form<pre>
 * CASE [expression]
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class SimpleCaseNode extends AbstractSelectExpression implements SelectExpression, ExpectedTypeAwareNode {

	public Type getDataType() {
		final AST expression = getFirstChild();
		// option is used to hold each WHEN/ELSE in turn
		AST option = expression.getNextSibling();
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
		return null;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	@Override
	public void setExpectedType(Type expectedType) {
		AST option = getFirstChild();
		while ( option != null ) {
			if ( option.getType() == HqlSqlTokenTypes.WHEN ) {
				if ( ParameterNode.class.isAssignableFrom( option.getFirstChild().getNextSibling().getClass() ) ) {
					((ParameterNode) option.getFirstChild().getNextSibling()).setExpectedType( expectedType );
				}
			}
			else if ( option.getType() == HqlSqlTokenTypes.ELSE ) {
				if ( ParameterNode.class.isAssignableFrom( option.getFirstChild().getClass() ) ) {
					((ParameterNode) option.getFirstChild()).setExpectedType( expectedType );
				}
			}
			option = option.getNextSibling();
		}
	}

	@Override
	public Type getExpectedType() {
		return null;
	}
}
