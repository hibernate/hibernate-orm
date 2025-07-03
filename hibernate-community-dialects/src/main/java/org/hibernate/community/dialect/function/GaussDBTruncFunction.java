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
 * Custom {@link TruncFunction} for GaussDB which uses the dialect-specific function for numeric truncation
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLTruncFunction.
 */
public class GaussDBTruncFunction extends TruncFunction {
	private final GaussDBTruncRoundFunction gaussDBTruncRoundFunction;

	public GaussDBTruncFunction(boolean supportsTwoArguments, TypeConfiguration typeConfiguration) {
		super(
				"trunc(?1)",
				null,
				DatetimeTrunc.DATE_TRUNC,
				null,
				typeConfiguration
		);
		this.gaussDBTruncRoundFunction = new GaussDBTruncRoundFunction( "trunc", supportsTwoArguments );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		if ( arguments.size() != 2 || !( arguments.get( 1 ) instanceof SqmExtractUnit ) ) {
			// numeric truncation
			return gaussDBTruncRoundFunction.generateSqmFunctionExpression(
					arguments,
					impliedResultType,
					queryEngine
			);
		}
		// datetime truncation
		return new SelfRenderingSqmFunction<>(
				this,
				datetimeRenderingSupport,
				args,
				impliedResultType,
				TruncArgumentsValidator.DATETIME_VALIDATOR,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}
}
