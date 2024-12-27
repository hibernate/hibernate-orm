/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

public class SDOMethodDescriptor extends OracleSpatialFunction {

	public SDOMethodDescriptor(
			String name,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, useParenthesesWhenNoArgs, argValidator, returnTypeResolver );
	}

	public SDOMethodDescriptor(
			String name,
			ArgumentsValidator argValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		this( name, true, argValidator, returnTypeResolver );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAstArguments.get(0).accept( walker );
		sqlAppender.appendSql( "." );
		sqlAppender.appendSql( getName() );
		//First argument is target of the method invocation
		if (this.alwaysIncludesParentheses() || sqlAstArguments.size() > 1) {
			sqlAppender.append( "()" );
		}
	}
}
