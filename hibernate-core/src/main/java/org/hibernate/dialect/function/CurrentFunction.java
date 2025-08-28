/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;

import java.util.List;

/**
 * A "function" with no parameters that returns the current date, time, or timestamp.
 * For example, {@code current_date}.
 *
 * @author Gavin King
 */
public class CurrentFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String sql;

	public CurrentFunction(String name, String sql, BasicType<?> type) {
		super(
				name,
				StandardArgumentsValidators.NO_ARGS,
				StandardFunctionReturnTypeResolvers.invariant( type ),
				null
		);
		this.sql = sql;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( sql );
	}

	@Override
	public String getArgumentListSignature() {
		return "";
	}

	@Override
	public boolean alwaysIncludesParentheses() {
		return sql.indexOf( '(' ) != -1;
	}
}
