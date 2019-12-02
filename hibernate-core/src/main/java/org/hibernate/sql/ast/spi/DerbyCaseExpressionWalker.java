/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;

/**
 * @author Andrea Boriero
 */
public class DerbyCaseExpressionWalker implements CaseExpressionWalker {

	public static DerbyCaseExpressionWalker INSTANCE = new DerbyCaseExpressionWalker();

	@Override
	public void visitCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression,
			StringBuilder sqlBuffer,
			SqlAstWalker sqlAstWalker) {

		sqlBuffer.append( "case " );

		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			sqlBuffer.append( " when " );
			whenFragment.getPredicate().accept( sqlAstWalker );
			sqlBuffer.append( " then " );
			whenFragment.getResult().accept( sqlAstWalker );
		}

		// TODO (6.0) : not sure this is the correct way to managed the otherwise expression
		Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			sqlBuffer.append( " else " );
			if ( otherwise instanceof QueryLiteral ) {
				Object value = ( (QueryLiteral) otherwise ).getLiteralValue();
				if ( value == null ) {
					// null is not considered the same type as Integer.
					sqlBuffer.append( "-1" );
				}
				else {
					otherwise.accept( sqlAstWalker );
				}
			}
			else {
				otherwise.accept( sqlAstWalker );
			}
		}
		sqlBuffer.append( " end" );
	}
}
