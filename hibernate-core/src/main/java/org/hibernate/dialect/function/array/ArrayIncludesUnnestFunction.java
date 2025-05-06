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
 * Implement the array includes function by using {@code unnest}.
 */
public class ArrayIncludesUnnestFunction extends AbstractArrayIncludesFunction {

	public ArrayIncludesUnnestFunction(boolean nullable, TypeConfiguration typeConfiguration) {
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
			walker.render( haystackExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( " is not null and " );
		}
		if ( ArrayHelper.isNullable( needleExpression ) ) {
			walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( " is not null and " );
		}
		if ( !nullable ) {
			sqlAppender.append( "not exists(select 1 from unnest(" );
			walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.append( ") t(i) where t.i is null) and " );
		}
		sqlAppender.append( "not exists(select * from unnest(" );
		walker.render( needleExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.append( ") except select * from unnest(" );
		walker.render( haystackExpression, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.append( ")))" );
	}

}
