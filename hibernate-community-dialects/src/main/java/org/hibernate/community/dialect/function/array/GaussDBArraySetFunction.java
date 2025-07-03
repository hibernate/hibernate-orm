/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import org.hibernate.dialect.function.array.ArrayAndElementArgumentTypeResolver;
import org.hibernate.dialect.function.array.ArrayAndElementArgumentValidator;
import org.hibernate.dialect.function.array.ArrayViaArgumentReturnTypeResolver;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;

/**
 * GaussDB array_set function.
 * @author chenzhida
 */
public class GaussDBArraySetFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public GaussDBArraySetFunction() {
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

		sqlAppender.append( "( SELECT array_agg( CASE WHEN idx_gen = ");
		indexExpression.accept( walker );
		sqlAppender.append( " THEN ");
		elementExpression.accept( walker );
		sqlAppender.append( " ELSE CASE  WHEN idx_gen <= array_length(ewa1_0.the_array, 1) ");
		sqlAppender.append( " THEN ewa1_0.the_array[idx_gen] ELSE NULL END END ORDER BY idx_gen ) ");
		sqlAppender.append( " FROM generate_series(1, GREATEST(COALESCE(array_length( ");
		arrayExpression.accept( walker );
		sqlAppender.append( " , 1), 0),  ");
		indexExpression.accept( walker );
		sqlAppender.append( " )) AS idx_gen ) AS result_array ");
	}
}
