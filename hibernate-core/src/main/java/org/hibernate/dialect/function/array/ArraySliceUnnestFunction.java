/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
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
 * Implement the array slice function by using {@code unnest}.
 */
public class ArraySliceUnnestFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean castEmptyArrayLiteral;

	public ArraySliceUnnestFunction(boolean castEmptyArrayLiteral) {
		super(
				"array_slice",
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( null, ANY, INTEGER, INTEGER ),
						ArrayArgumentValidator.DEFAULT_INSTANCE
				),
				ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE,
				StandardFunctionArgumentTypeResolvers.composite(
						StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER, INTEGER ),
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
		final Expression startIndexExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression endIndexExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "case when ");
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null and ");
		startIndexExpression.accept( walker );
		sqlAppender.append( " is not null and ");
		endIndexExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(t.val) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx) where t.idx between " );
		startIndexExpression.accept( walker );
		sqlAppender.append( " and " );
		endIndexExpression.accept( walker );
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
