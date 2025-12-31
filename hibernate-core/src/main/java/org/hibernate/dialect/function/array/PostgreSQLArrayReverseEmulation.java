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
 * PostgreSQL array_reverse emulation for versions before 18.
 * HSQLDB uses the same approach.
 */
public class PostgreSQLArrayReverseEmulation extends AbstractArrayReverseFunction {

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final SqlAstNode arrayExpression = sqlAstArguments.get( 0 );

		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then " );
		sqlAppender.append( "coalesce((select array_agg(t.val order by t.idx desc) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx)" );

		String arrayTypeName = null;
		if ( returnType instanceof BasicPluralType<?, ?> pluralType ) {
			if ( needsArrayCasting( pluralType.getElementType() ) ) {
				arrayTypeName = DdlTypeHelper.getCastTypeName(
						returnType,
						walker.getSessionFactory().getJdbcServices().getDialect(),
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
		sqlAppender.append( " end" );
	}

	private static boolean needsArrayCasting(BasicType<?> elementType) {
		return elementType.getJdbcType().isString();
	}
}
