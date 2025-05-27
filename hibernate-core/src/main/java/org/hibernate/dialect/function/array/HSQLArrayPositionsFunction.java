/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * HSQLDB has a special syntax.
 */
public class HSQLArrayPositionsFunction extends AbstractArrayPositionsFunction {

	public HSQLArrayPositionsFunction(boolean list, TypeConfiguration typeConfiguration) {
		super( list, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 1 );
		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(t.idx) from unnest(");
		arrayExpression.accept( walker );
		sqlAppender.append(") with ordinality t(val,idx) where t.val is not distinct from " );
		walker.render( elementExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.append( "),cast(array[] as integer array)) end" );
	}
}
