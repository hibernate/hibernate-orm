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
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;

/**
 * Emulation of PostgreSQL's array_remove function for Spanner PostgreSQL dialect.
 */
public class SpannerPostgreSQLArrayRemoveFunction extends AbstractArrayRemoveFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 1 );

		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then coalesce((select array_agg(t.val order by t.idx) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality as t(val, idx) where t.val != " );
		elementExpression.accept( walker );
		sqlAppender.append( " or (t.val is null and " );
		elementExpression.accept( walker );
		sqlAppender.append( " is not null) or (t.val is not null and " );
		elementExpression.accept( walker );
		sqlAppender.append( " is null))" );

		String arrayTypeName = null;
		if ( returnType instanceof BasicPluralType<?, ?> pluralType ) {
			if ( needsArrayCasting( pluralType.getElementType() ) ) {
				arrayTypeName = org.hibernate.dialect.function.array.DdlTypeHelper.getCastTypeName(
						returnType,
						walker.getSessionFactory().getTypeConfiguration()
				);
			}
		}
		if ( arrayTypeName != null ) {
			sqlAppender.append( ",cast(array[] as " );
			sqlAppender.appendSql( arrayTypeName );
			sqlAppender.appendSql( ")) end" );
		}
		else {
			sqlAppender.append( ",array[]) end" );
		}
	}

	private static boolean needsArrayCasting(BasicType<?> elementType) {
		// PostgreSQL doesn't do implicit conversion between text[] and varchar[], so we need casting
		return elementType.getJdbcType().isString();
	}
}
