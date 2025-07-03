/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.array.AbstractArrayFillFunction;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;

/**
 * Custom casting for the array fill function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLArrayFillFunction.
 */
public class GaussDBArrayFillFunction extends AbstractArrayFillFunction {

	public GaussDBArrayFillFunction(boolean list) {
		super( list );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "array_fill(" );
		final String elementCastType;
		final Expression elementExpression = (Expression) sqlAstArguments.get( 0 );
		if ( needsElementCasting( elementExpression ) ) {
			elementCastType = DdlTypeHelper.getCastTypeName(
					elementExpression.getExpressionType(),
					walker.getSessionFactory().getTypeConfiguration()
			);
			sqlAppender.append( "cast(" );
		}
		else {
			elementCastType = null;
		}
		sqlAstArguments.get( 0 ).accept( walker );
		if ( elementCastType != null ) {
			sqlAppender.append( " as " );
			sqlAppender.append( elementCastType );
			sqlAppender.append( ')' );
		}
		sqlAppender.append( ",array[" );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( "])" );
	}

	private static boolean needsElementCasting(Expression elementExpression) {
		// GaussDB needs casting of null and string literal expressions
		return elementExpression instanceof Literal && (
				elementExpression.getExpressionType().getSingleJdbcMapping().getJdbcType().isString()
						|| ( (Literal) elementExpression ).getLiteralValue() == null
		);
	}
}
