/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Implement the intersects function by using {@code unnest}.
 */
public class ArrayIntersectsUnnestFunction extends AbstractArrayIntersectsFunction {

	public ArrayIntersectsUnnestFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super( nullable, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression haystackExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression needleExpression = (Expression) sqlAstArguments.get( 1 );
		sqlAppender.append( '(' );
		if ( ArrayHelper.isNullable( haystackExpression ) ) {
			haystackExpression.accept( walker );
			sqlAppender.append( " is not null and " );
		}
		if ( ArrayHelper.isNullable( needleExpression ) ) {
			needleExpression.accept( walker );
			sqlAppender.append( " is not null and " );
		}
		if ( !nullable ) {
			sqlAppender.append( "not exists(select 1 from unnest(" );
			needleExpression.accept( walker );
			sqlAppender.append( ") t(i) where t.i is null) and " );
		}
		sqlAppender.append( "exists(select * from unnest(" );
		needleExpression.accept( walker );
		sqlAppender.append( ")" );
		sqlAppender.append( " intersect " );
		sqlAppender.append( "select * from unnest(" );
		haystackExpression.accept( walker );
		sqlAppender.append( ")))" );
	}
}
