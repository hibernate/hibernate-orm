/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Oracle concatenation function for arrays.
 */
public class OracleArrayConcatFunction extends ArrayConcatFunction {

	public OracleArrayConcatFunction() {
		super( "(", ",", ")" );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				(JdbcMappingContainer) returnType,
				walker.getSessionFactory().getTypeConfiguration()
		);
		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_concat" );
		super.render( sqlAppender, sqlAstArguments, returnType, walker );
	}
}
