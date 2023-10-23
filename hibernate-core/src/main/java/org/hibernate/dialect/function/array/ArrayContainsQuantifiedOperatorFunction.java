/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Array contains all function that uses the PostgreSQL {@code @>} operator.
 */
public class ArrayContainsQuantifiedOperatorFunction extends ArrayContainsQuantifiedUnnestFunction {

	public ArrayContainsQuantifiedOperatorFunction(TypeConfiguration typeConfiguration, boolean all, boolean nullable) {
		super( typeConfiguration, all, nullable );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		if ( nullable ) {
			super.render( sqlAppender, sqlAstArguments, walker );
		}
		else {
			final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
			final Expression needleExpression = (Expression) sqlAstArguments.get( 1 );
			haystackExpression.accept( walker );
			if ( all ) {
				sqlAppender.append( "@>" );
			}
			else {
				sqlAppender.append( "&&" );
			}
			needleExpression.accept( walker );
		}
	}
}
