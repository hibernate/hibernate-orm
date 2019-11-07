/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.List;

import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Andrea Boriero
 */
public class DecodeCaseExpressionWalker implements CaseExpressionWalker {

	public static final DecodeCaseExpressionWalker INSTANCE = new DecodeCaseExpressionWalker();

	@Override
	public void visitCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression, StringBuilder sqlBuffer, SqlAstWalker sqlAstWalker) {
		sqlBuffer.append( "decode( " );

		List<CaseSearchedExpression.WhenFragment> whenFragments = caseSearchedExpression.getWhenFragments();
		int caseNumber = whenFragments.size();
		for ( int i = 0; i < caseNumber; i++ ) {
			final CaseSearchedExpression.WhenFragment whenFragment = whenFragments.get( i );
			if ( i != 0 ) {
				sqlBuffer.append( ", " );
			}
			whenFragment.getPredicate().getLeftHandExpression().accept( sqlAstWalker );
			sqlBuffer.append( ", " );
			whenFragment.getResult().accept( sqlAstWalker );
		}

		Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			sqlBuffer.append( ", " );
			otherwise.accept( sqlAstWalker );
		}

		sqlBuffer.append( "')'" );

		final String columnExpression = caseSearchedExpression.getColumnExpression();

		if ( columnExpression != null ) {
			sqlBuffer.append( " as " ).append( columnExpression );
		}
	}
}
