/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;


import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 * The trunc function for Spanner.
 * It renders DATE_TRUNC for dates, TIMESTAMP_TRUNC for timestamps, and TRUNC for numeric types.
 */
public class SpannerTruncFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public SpannerTruncFunction() {
		super(
				"trunc",
				new TruncFunction.TruncArgumentsValidator(),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression expression = (Expression) sqlAstArguments.get( 0 );
		final var type = expression.getExpressionType();
		final var temporalType = type != null ? getSqlTemporalType( type ) : null;

		if ( temporalType != null ) {
			switch ( temporalType ) {
				case DATE -> sqlAppender.appendSql( "DATE_TRUNC" );
				case TIMESTAMP, TIME -> sqlAppender.appendSql( "TIMESTAMP_TRUNC" );
				default -> throw new IllegalArgumentException( "Unsupported temporal type: " + temporalType );
			}
			sqlAppender.appendSql( "(" );
			expression.accept( walker );
			sqlAppender.appendSql( ", " );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ")" );
		}
		else {
			renderNumericTrunc( sqlAppender, sqlAstArguments, walker );
		}
	}

	private void renderNumericTrunc(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> args,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "TRUNC(" );
		args.get( 0 ).accept( walker );
		if ( args.size() > 1 ) {
			sqlAppender.appendSql( ", " );
			args.get( 1 ).accept( walker );
		}
		sqlAppender.appendSql( ")" );
	}
}
