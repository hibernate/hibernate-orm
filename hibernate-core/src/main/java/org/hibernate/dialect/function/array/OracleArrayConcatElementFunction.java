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

/**
 * Oracle concatenation function for array and an element.
 */
public class OracleArrayConcatElementFunction extends ArrayConcatElementFunction {

	public OracleArrayConcatElementFunction(boolean prepend) {
		super( "(", ",", ")", prepend );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression firstArgument = (Expression) sqlAstArguments.get( 0 );
		final Expression secondArgument = (Expression) sqlAstArguments.get( 1 );
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				prepend ? secondArgument.getExpressionType()
						: firstArgument.getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_concat(" );
		if ( prepend ) {
			sqlAppender.append( arrayTypeName );
			sqlAppender.append( '(' );
			firstArgument.accept( walker );
			sqlAppender.append( ')' );
		}
		else {
			firstArgument.accept( walker );
		}
		sqlAppender.append( ',' );
		if ( prepend ) {
			secondArgument.accept( walker );
		}
		else {
			sqlAppender.append( arrayTypeName );
			sqlAppender.append( '(' );
			secondArgument.accept( walker );
			sqlAppender.append( ')' );
		}
		sqlAppender.append( ')' );
	}
}
