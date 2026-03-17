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
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;

/**
 * Spanner PostgreSQL emulation for array_trim.
 */
public class SpannerPostgreSQLArrayTrimEmulation extends AbstractArrayTrimFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final SqlAstNode arrayExpression = sqlAstArguments.get( 0 );
		final SqlAstNode elementCountExpression = sqlAstArguments.get( 1 );
		sqlAppender.append( "coalesce((select array_agg(t.val order by t.idx) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx) where t.idx<=coalesce(array_length(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ", 1), 0)-" );
		elementCountExpression.accept( walker );

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
			sqlAppender.append( "),cast(array[] as " );
			sqlAppender.appendSql( arrayTypeName );
			sqlAppender.appendSql( "))" );
		}
		else {
			sqlAppender.append( "),array[])" );
		}
	}

	private static boolean needsArrayCasting(BasicType<?> elementType) {
		// PostgreSQL doesn't do implicit conversion between text[] and varchar[], so we need casting
		return elementType.getJdbcType().isString();
	}
}
