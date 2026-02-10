/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Emulation of locate() for Spanner using strpos().
 * Handles the swapped argument order between HQL locate() and Spanner strpos(),
 * and emulates the optional 3rd argument (start position) using substr().
 */
public class SpannerLocateEmulation extends AbstractSqmFunctionDescriptor {

	public SpannerLocateEmulation(TypeConfiguration typeConfiguration) {
		super(
				"locate",
				StandardArgumentsValidators.between( 2, 3 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				),
				null
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {

		return new SelfRenderingSqmFunction<>(
				this,
				this::render,
				arguments,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	private void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.size() == 2 ) {
			sqlAppender.append( "strpos(" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ", " );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.append( ")" );
		}
		else if ( sqlAstArguments.size() == 3 ) {
			sqlAppender.append( "(" );
			sqlAppender.append( "strpos(substr(" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ", " );
			sqlAstArguments.get( 2 ).accept( walker );
			sqlAppender.append( "), " );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.append( ")" );

			sqlAppender.append( " + case when strpos(substr(" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.append( ", " );
			sqlAstArguments.get( 2 ).accept( walker );
			sqlAppender.append( "), " );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.append( ") > 0 then " );
			sqlAstArguments.get( 2 ).accept( walker );
			sqlAppender.append( " - 1 else 0 end)" );
		}
	}
}
