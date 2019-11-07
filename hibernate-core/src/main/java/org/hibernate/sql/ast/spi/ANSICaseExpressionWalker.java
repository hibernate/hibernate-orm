/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Andrea Boriero
 */
public class ANSICaseExpressionWalker implements CaseExpressionWalker {

	public static ANSICaseExpressionWalker INSTANCE = new ANSICaseExpressionWalker();

	public void visitCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression,
			StringBuilder sqlBuffer,
			SqlAstWalker sqlAstWalker){
		sqlBuffer.append( "case " );

		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			sqlBuffer.append( " when " );
			whenFragment.getPredicate().accept( sqlAstWalker );
			sqlBuffer.append( " then " );
			whenFragment.getResult().accept( sqlAstWalker );
		}

		Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			sqlBuffer.append( " else " );
			otherwise.accept( sqlAstWalker );
		}

		sqlBuffer.append( " end" );

		final String columnExpression = caseSearchedExpression.getColumnExpression();

		if ( columnExpression != null ) {
			sqlBuffer.append( " as " ).append( columnExpression );
		}
	}
}
