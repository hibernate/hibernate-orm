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
 * Implement the array remove index function by using {@code unnest}.
 */
public class ArrayRemoveIndexUnnestFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean castEmptyArrayLiteral;

	public ArrayRemoveIndexUnnestFunction(boolean castEmptyArrayLiteral) {
		super(
				"array_remove_index",
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( null, ANY, INTEGER ),
						ArrayArgumentValidator.DEFAULT_INSTANCE
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER ),
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
				)
		);
		this.castEmptyArrayLiteral = castEmptyArrayLiteral;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression indexExpression = (Expression) sqlAstArguments.get( 1 );
		sqlAppender.append( "case when ");
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(t.val) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx) where t.idx is distinct from " );
		indexExpression.accept( walker );
		sqlAppender.append( "),");
		if ( castEmptyArrayLiteral ) {
			sqlAppender.append( "cast(array[] as " );
			sqlAppender.append( DdlTypeHelper.getCastTypeName(
					returnType,
					walker.getSessionFactory().getTypeConfiguration()
			) );
			sqlAppender.append( ')' );
		}
		else {
			sqlAppender.append( "array[]" );
		}
		sqlAppender.append(") end" );
	}
}
