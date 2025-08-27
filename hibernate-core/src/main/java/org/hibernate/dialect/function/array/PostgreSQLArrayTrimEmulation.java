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
 * PostgreSQL array_trim emulation, since the function was only introduced in version 14.
 */
public class PostgreSQLArrayTrimEmulation extends AbstractArrayTrimFunction {

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
		sqlAppender.append( ") with ordinality t(val,idx) where t.idx<=cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ")-" );
		elementCountExpression.accept( walker );

		String arrayTypeName = null;
		if ( returnType instanceof BasicPluralType<?, ?> pluralType ) {
			if ( needsArrayCasting( pluralType.getElementType() ) ) {
				arrayTypeName = DdlTypeHelper.getCastTypeName(
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
