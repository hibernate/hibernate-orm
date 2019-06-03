/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.type.spi.TypeConfiguration.isTimestampType;

/**
 * @author Gavin King
 */
public class CurrentFunction
		extends AbstractSqmFunctionTemplate implements SelfRenderingFunctionSupport {

	private final String name;
	private final String sql;

	public CurrentFunction(String name, String sql, StandardBasicTypes.StandardBasicType type) {
		super(
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
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqmFunction<>(
				this,
				arguments,
				impliedResultType,
				queryEngine.getCriteriaBuilder(),
				name
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "";
	}

}
