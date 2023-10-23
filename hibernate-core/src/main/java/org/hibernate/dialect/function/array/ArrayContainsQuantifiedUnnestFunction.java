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
 * Implement the contains all function by using {@code unnest}.
 */
public class ArrayContainsQuantifiedUnnestFunction extends AbstractArrayContainsQuantifiedFunction {

	protected final boolean all;
	protected final boolean nullable;

	public ArrayContainsQuantifiedUnnestFunction(TypeConfiguration typeConfiguration, boolean all, boolean nullable) {
		super( "array_contains_" + ( all ? "all" : "any" ), typeConfiguration );
		this.all = all;
		this.nullable = nullable;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression needleExpression = (Expression) sqlAstArguments.get( 1 );
		sqlAppender.append( '(' );
		haystackExpression.accept( walker );
		sqlAppender.append( " is not null and " );
		needleExpression.accept( walker );
		sqlAppender.append( " is not null and " );
		if ( !nullable ) {
			sqlAppender.append( "not exists(select 1 from unnest(" );
			needleExpression.accept( walker );
			sqlAppender.append( ") t(i) where t.i is null) and " );
		}
		if ( all ) {
			sqlAppender.append( "not " );
		}
		sqlAppender.append( "exists(select * from unnest(" );
		needleExpression.accept( walker );
		sqlAppender.append( ")" );
		if ( all ) {
			sqlAppender.append( " except " );
		}
		else {
			sqlAppender.append( " intersect " );
		}
		sqlAppender.append( "select * from unnest(" );
		haystackExpression.accept( walker );
		sqlAppender.append( ")))" );
	}
}
