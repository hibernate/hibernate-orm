/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;

/**
 * Custom casting for the array fill function.
 */
public class PostgreSQLArrayFillFunction extends AbstractArrayFillFunction {

	public PostgreSQLArrayFillFunction(boolean list) {
		super( list );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "array_fill(" );
		final String elementCastType;
		final Expression elementExpression = (Expression) sqlAstArguments.get( 0 );
		if ( needsElementCasting( elementExpression ) ) {
			elementCastType = DdlTypeHelper.getCastTypeName(
					elementExpression.getExpressionType(),
					walker.getSessionFactory().getTypeConfiguration()
			);
			sqlAppender.append( "cast(" );
		}
		else {
			elementCastType = null;
		}
		sqlAstArguments.get( 0 ).accept( walker );
		if ( elementCastType != null ) {
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		sqlAppender.append( ",array[" );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( "])" );
	}

	private static boolean needsElementCasting(Expression elementExpression) {
		// PostgreSQL needs casting of null and string literal expressions
		return elementExpression instanceof Literal && (
				elementExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString()
						|| ( (Literal) elementExpression ).getLiteralValue() == null
		);
	}
}
