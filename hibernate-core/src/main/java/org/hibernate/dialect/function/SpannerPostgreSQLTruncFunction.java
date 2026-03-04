/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Spanner-specific TruncFunction that extends PostgreSQLTruncFunction to handle
 * both numeric and datetime truncation, reusing PostgreSQL logic where
 * applicable.
 */
public class SpannerPostgreSQLTruncFunction extends PostgreSQLTruncFunction {
	private final SpannerPostgreSQLTruncRoundFunction spannerPostgreSQLTruncRoundFunction;

	public SpannerPostgreSQLTruncFunction(TypeConfiguration typeConfiguration) {
		super(false, typeConfiguration);
		this.spannerPostgreSQLTruncRoundFunction = new SpannerPostgreSQLTruncRoundFunction();
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final List<SqmTypedNode<?>> args = new ArrayList<>(arguments);
		if (arguments.size() != 2 || !(arguments.get(1) instanceof SqmExtractUnit)) {
			// numeric truncation - delegate to Spanner-specific implementation
			return spannerPostgreSQLTruncRoundFunction.generateSqmFunctionExpression(
					arguments,
					impliedResultType,
					queryEngine);
		}
		// datetime truncation - delegate to parent (PostgreSQLTruncFunction) which
		// handles it correctly
		return super.generateSqmFunctionExpression(arguments, impliedResultType, queryEngine);
	}
}
