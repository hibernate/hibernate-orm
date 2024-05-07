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
 * H2 requires a very special emulation, because {@code unnest} is pretty much useless,
 * due to https://github.com/h2database/h2database/issues/1815.
 * This emulation uses {@code array_get}, {@code array_length} and {@code system_range} functions to roughly achieve the same.
 */
public class H2ArrayToStringFunction extends ArrayToStringFunction {

	private final int maximumArraySize;

	public H2ArrayToStringFunction(int maximumArraySize, TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression separatorExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression defaultExpression = sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select listagg(" );
		if ( defaultExpression != null ) {
			sqlAppender.append( "coalesce(" );
		}
		sqlAppender.append( "array_get(" );
		arrayExpression.accept( walker );
		sqlAppender.append(",i.idx)" );
		if ( defaultExpression != null ) {
			sqlAppender.append( "," );
			defaultExpression.accept( walker );
			sqlAppender.append( ")" );
		}
		sqlAppender.append("," );
		separatorExpression.accept( walker );
		sqlAppender.append( ") within group (order by i.idx) from system_range(1,");
		sqlAppender.append( Integer.toString( maximumArraySize ) );
		sqlAppender.append( ") i(idx) where i.idx<=coalesce(cardinality(");
		arrayExpression.accept( walker );
		sqlAppender.append("),0)),'') end" );
	}
}
