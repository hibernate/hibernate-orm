/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * HSQLDB has a special syntax.
 */
public class HSQLArrayPositionFunction extends AbstractArrayPositionFunction {

	public HSQLArrayPositionFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
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
		sqlAppender.append( " is not null then coalesce((select t.idx from unnest(");
		arrayExpression.accept( walker );
		sqlAppender.append(") with ordinality t(val,idx) where t.val is not distinct from " );
		walker.render( elementExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		if ( sqlAstArguments.size() > 2 ) {
			sqlAppender.append( " and t.idx>=" );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.append( " order by t.idx fetch first 1 row only),0) end" );
	}
}
