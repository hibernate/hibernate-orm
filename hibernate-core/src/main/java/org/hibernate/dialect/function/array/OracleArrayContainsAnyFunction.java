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

public class OracleArrayContainsAnyFunction extends AbstractArrayContainsQuantifiedFunction {

	private final boolean nullable;

	public OracleArrayContainsAnyFunction(TypeConfiguration typeConfiguration, boolean nullable) {
		super( "array_contains_any", typeConfiguration );
		this.nullable = nullable;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
		final String arrayTypeName = DdlTypeHelper.getTypeName( haystackExpression.getExpressionType(), walker );
		sqlAppender.appendSql( arrayTypeName );
		sqlAppender.append( "_contains_any(" );
		haystackExpression.accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAppender.append( nullable ? "1" : "0" );
		sqlAppender.append( ")>0" );
	}

}
