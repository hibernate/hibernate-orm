/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

public class OracleSpatialFunction extends NamedSqmFunctionDescriptor {

	public OracleSpatialFunction(String name, boolean useParenthesesWhenNoArgs, ArgumentsValidator argValidator, FunctionReturnTypeResolver returnTypeResolver){
		super( name, useParenthesesWhenNoArgs, argValidator, returnTypeResolver );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		super.render( sqlAppender, sqlAstArguments, returnType, walker );
	}
}
