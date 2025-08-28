/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A special function that renders a T-SQL {@code str()} function
 * if more than a single argument is given, or otherwise renders
 * a {@code cast()} expression just like {@link CastStrEmulation}.
 *
 * @author Christian Beikov
 */
public class TransactSQLStrFunction extends CastStrEmulation implements FunctionRenderer {

	public TransactSQLStrFunction(TypeConfiguration typeConfiguration) {
		super(
				"str",
				StandardArgumentsValidators.between( 1, 3 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		if ( arguments.size() == 1 ) {
			return super.generateSqmFunctionExpression(
					arguments,
					impliedResultType,
					queryEngine
			);
		}

		return new SelfRenderingSqmFunction<>(
				this,
				this,
				arguments,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "str(" );
		arguments.get( 0 ).accept( walker );
		for ( int i = 1; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( ',' );
			arguments.get( i ).accept( walker );
		}
		sqlAppender.appendSql( ')' );
	}
}
