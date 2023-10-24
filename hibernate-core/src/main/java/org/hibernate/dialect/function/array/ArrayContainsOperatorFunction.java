/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Special array contains function that also applies a cast to the element argument. PostgreSQL needs this,
 * because by default it assumes a {@code text[]}, which is not compatible with {@code varchar[]}.
 */
public class ArrayContainsOperatorFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public ArrayContainsOperatorFunction(TypeConfiguration typeConfiguration) {
		super(
				"array_contains",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						ArrayAndElementArgumentValidator.DEFAULT_INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Boolean.class ) ),
				ArrayAndElementArgumentTypeResolver.DEFAULT_INSTANCE
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 1 );
		arrayExpression.accept( walker );
		sqlAppender.append( "@>" );
		if ( needsArrayCasting( elementExpression ) ) {
			sqlAppender.append( "cast(array[" );
			elementExpression.accept( walker );
			sqlAppender.append( "] as " );
			sqlAppender.append( DdlTypeHelper.getCastTypeName( arrayExpression.getExpressionType(), walker ) );
			sqlAppender.append( ')' );
		}
		else {
			sqlAppender.append( "array[" );
			elementExpression.accept( walker );
			sqlAppender.append( ']' );
		}
	}

	private static boolean needsArrayCasting(Expression elementExpression) {
		// PostgreSQL doesn't do implicit conversion between text[] and varchar[], so we need casting
		return elementExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString();
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY array, OBJECT element)";
	}
}
