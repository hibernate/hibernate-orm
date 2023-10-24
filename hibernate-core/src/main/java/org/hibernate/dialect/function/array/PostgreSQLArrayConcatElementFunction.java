/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BottomType;

/**
 * PostgreSQL variant of the function to properly return {@code null} when the array argument is null.
 */
public class PostgreSQLArrayConcatElementFunction extends ArrayConcatElementFunction {

	public PostgreSQLArrayConcatElementFunction(boolean prepend) {
		super( "", "||", "", prepend );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final Expression firstArgument = (Expression) sqlAstArguments.get( 0 );
		final Expression secondArgument = (Expression) sqlAstArguments.get( 1 );
		final Expression arrayArgument;
		final Expression elementArgument;
		if ( prepend ) {
			elementArgument = firstArgument;
			arrayArgument = secondArgument;
		}
		else {
			arrayArgument = firstArgument;
			elementArgument = secondArgument;
		}
		final String elementCastType;
		if ( needsElementCasting( elementArgument ) ) {
			final JdbcMapping elementType = elementArgument.getExpressionType().getSingleJdbcMapping();
			if ( elementType instanceof BottomType  ) {
				elementCastType = DdlTypeHelper.getCastTypeName(
						( (BasicPluralType<?, ?>) arrayArgument.getExpressionType().getSingleJdbcMapping() )
								.getElementType(),
						walker
				);
			}
			else {
				elementCastType = DdlTypeHelper.getCastTypeName( elementType, walker );
			}
		}
		else {
			elementCastType = null;
		}
		sqlAppender.append( "case when " );
		arrayArgument.accept( walker );
		sqlAppender.append( " is not null then " );
		if ( prepend && elementCastType != null) {
			sqlAppender.append( "cast(" );
			firstArgument.accept( walker );
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		else {
			firstArgument.accept( walker );
		}
		sqlAppender.append( "||" );
		if ( !prepend && elementCastType != null) {
			sqlAppender.append( "cast(" );
			secondArgument.accept( walker );
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		else {
			secondArgument.accept( walker );
		}
		sqlAppender.append( " end" );
	}

	private static boolean needsElementCasting(Expression elementExpression) {
		// PostgreSQL needs casting of null and string literal expressions
		return elementExpression instanceof Literal && (
				elementExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString()
						|| ( (Literal) elementExpression ).getLiteralValue() == null
		);
	}
}
