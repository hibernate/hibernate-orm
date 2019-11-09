/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;

/**
 * @author Andrea Boriero
 */
public class MckoiCaseExpressionWalker implements CaseExpressionWalker {

	public static final MckoiCaseExpressionWalker INSTANCE = new MckoiCaseExpressionWalker();

	@Override
	public void visitCaseSearchedExpression(
			CaseSearchedExpression caseSearchedExpression, StringBuilder sqlBuffer, SqlAstWalker sqlAstWalker) {
		sqlBuffer.append( "case " );
		StringBuilder buf2= new StringBuilder( );

		for ( CaseSearchedExpression.WhenFragment whenFragment : caseSearchedExpression.getWhenFragments() ) {
			sqlBuffer.append( " if( " );
			whenFragment.getPredicate().accept( sqlAstWalker );
			sqlBuffer.append( ", " );
			whenFragment.getResult().accept( sqlAstWalker );
			sqlBuffer.append(", ");
			buf2.append(")");
		}
		Expression otherwise = caseSearchedExpression.getOtherwise();
		if ( otherwise != null ) {
			otherwise.accept( sqlAstWalker );
		}
		else {
			sqlBuffer.append( "null" );
		}
		sqlBuffer.append(buf2);
	}
}
