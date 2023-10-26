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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Array overlaps function that uses the PostgreSQL {@code &&} operator.
 */
public class ArrayOverlapsOperatorFunction extends ArrayOverlapsUnnestFunction {

	public ArrayOverlapsOperatorFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super( nullable, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
		if ( nullable ) {
			super.render( sqlAppender, sqlAstArguments, returnType, walker );
		}
		else {
			final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
			final Expression needleExpression = (Expression) sqlAstArguments.get( 1 );
			haystackExpression.accept( walker );
			sqlAppender.append( "&&" );
			needleExpression.accept( walker );
		}
	}
}
