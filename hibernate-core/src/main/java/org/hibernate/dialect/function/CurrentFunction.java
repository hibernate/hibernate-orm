/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingSqlFunctionExpression;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * @author Gavin King
 */
public class CurrentFunction
		extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {

	private final String name;
	private final String sql;

	public CurrentFunction(String name, String sql, BasicType type) {
		super(
				name,
				StandardArgumentsValidators.NO_ARGS,
				StandardFunctionReturnTypeResolvers.invariant( type )
		);
		this.name = name;
		this.sql = sql;
	}

		@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		sqlAppender.appendSql(sql);
	}

	@Override
	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqlFunctionExpression<T>(
				this, this,
				arguments,
				impliedResultType,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				name
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "";
	}

}
