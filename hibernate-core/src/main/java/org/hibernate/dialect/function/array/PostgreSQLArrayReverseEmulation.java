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

		sqlAppender.append( "coalesce((select array_agg(t.val order by t.idx desc) from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val,idx))," );

		arrayExpression.accept( walker );
		sqlAppender.append( ")" );

	}
}
