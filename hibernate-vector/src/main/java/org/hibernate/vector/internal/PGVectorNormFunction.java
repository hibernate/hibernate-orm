/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

public class PGVectorNormFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
	public PGVectorNormFunction(TypeConfiguration typeConfiguration) {
		super(
				"vector_norm",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 1 ),
						VectorArgumentValidator.INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.getBasicTypeForJavaType( Double.class ) ),
				VectorArgumentTypeResolver.INSTANCE
		);
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
		final Expression expression = (Expression) sqlAstArguments.get( 0 );
		sqlAppender.append(
				switch ( expression.getExpressionType().getSingleJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case SqlTypes.SPARSE_VECTOR_FLOAT32, SqlTypes.VECTOR_FLOAT16 -> "l2_norm";
					default -> "vector_norm";
				}
		);
		sqlAppender.append( '(' );
		expression.accept( walker );
		sqlAppender.append( ')' );
	}
}
