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

/**
 * Oracle array_remove_index function.
 */
public class OracleArrayRemoveIndexFunction extends ArrayRemoveIndexUnnestFunction {

	public OracleArrayRemoveIndexFunction() {
		super( false );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				( (Expression) sqlAstArguments.get( 0 ) ).getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_remove_index(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ')' );
	}
}
