/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

/**
 * Implement the array set function by using {@code unnest}.
 */
public class ArraySetUnnestFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public ArraySetUnnestFunction() {
		super(
				"array_set",
				StandardArgumentsValidators.composite(
						new ArrayAndElementArgumentValidator( 0, 2 ),
						new ArgumentTypesValidator( null, ANY, INTEGER, ANY )
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE,
						StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER, ANY ),
						new ArrayAndElementArgumentTypeResolver( 0, 2 )
				)
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression indexExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "(select array_agg(case when i.idx=");
		indexExpression.accept( walker );
		sqlAppender.append(" then " );
		elementExpression.accept( walker );
		sqlAppender.append(" else t.val end) from generate_series(1,greatest(coalesce(cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( "),0)," );
		indexExpression.accept( walker );
		sqlAppender.append( ")) i(idx) left join unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val, idx) on i.idx=t.idx)" );
	}
}
