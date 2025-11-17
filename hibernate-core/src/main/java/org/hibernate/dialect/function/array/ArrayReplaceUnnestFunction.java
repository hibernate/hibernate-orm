/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * Implement the array replace function by using {@code unnest}.
 */
public class ArrayReplaceUnnestFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public ArrayReplaceUnnestFunction() {
		super(
				"array_replace",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 3 ),
						new ArrayAndElementArgumentValidator( 0, 1, 2 )
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				new ArrayAndElementArgumentTypeResolver( 0, 1, 2 )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression oldExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression newExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "case when ");
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(case when t.val is not distinct from " );
		oldExpression.accept( walker );
		sqlAppender.append( " then " );
		newExpression.accept( walker );
		sqlAppender.append( " else t.val end) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") t(val)),array[]) end" );
	}
}
