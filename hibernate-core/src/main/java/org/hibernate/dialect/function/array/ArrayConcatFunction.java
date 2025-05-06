/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Concatenation function for arrays.
 */
public class ArrayConcatFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String prefix;
	private final String separator;
	private final String suffix;

	public ArrayConcatFunction(String prefix, String separator, String suffix) {
		super(
				"array_concat",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.min( 2 ),
						ArraysOfSameTypeArgumentValidator.INSTANCE
				),
				StandardFunctionReturnTypeResolvers.useFirstNonNull(),
				StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE
		);
		this.prefix = prefix;
		this.separator = separator;
		this.suffix = suffix;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( prefix );
		sqlAstArguments.get( 0 ).accept( walker );
		for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
			sqlAppender.append( separator );
			sqlAstArguments.get( i ).accept( walker );
		}
		sqlAppender.append( suffix );
	}
}
