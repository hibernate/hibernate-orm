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
 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
 * due to https://github.com/h2database/h2database/issues/1815.
 * This emulation uses {@code array_get}, {@code array_length} and {@code system_range} functions to roughly achieve the same.
 */
public class H2ArrayContainsQuantifiedEmulation extends AbstractArrayContainsQuantifiedFunction {

	private final boolean all;
	private final boolean nullable;

	public H2ArrayContainsQuantifiedEmulation(TypeConfiguration typeConfiguration, boolean all, boolean nullable) {
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
			sqlAppender.append( "not array_contains(" );
			needleExpression.accept( walker );
			sqlAppender.append( ",null) and " );
		}
		if ( all ) {
			sqlAppender.append( "not " );
		}
		sqlAppender.append( "exists(select array_get(" );
		needleExpression.accept( walker );
		sqlAppender.append( ",t.i) from system_range(1," );
		sqlAppender.append( Integer.toString( getMaximumArraySize() ) );
		sqlAppender.append( ") t(i) where array_length(" );
		needleExpression.accept( walker );
		sqlAppender.append( ")>=t.i" );
		if ( all ) {
			sqlAppender.append( " except " );
		}
		else {
			sqlAppender.append( " intersect " );
		}
		sqlAppender.append( "select array_get(" );
		haystackExpression.accept( walker );
		sqlAppender.append( ",t.i) from system_range(1," );
		sqlAppender.append( Integer.toString( getMaximumArraySize() ) );
		sqlAppender.append( ") t(i) where array_length(" );
		haystackExpression.accept( walker );
		sqlAppender.append( ")>=t.i))" );
	}

	protected int getMaximumArraySize() {
		return 1000;
	}

}
