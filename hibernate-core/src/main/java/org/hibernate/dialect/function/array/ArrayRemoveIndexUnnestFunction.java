/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Expression;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Subclassable `array_remove_index` descriptor implemented using `unnest`.
@SPI({ USE, IMPLEMENT })
public class ArrayRemoveIndexUnnestFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean castEmptyArrayLiteral;

	@SPI(IMPLEMENT)
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
		sqlAppender.append( " is not null then coalesce((select array_agg(t.val order by t.idx) from unnest(" );
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
