/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import java.util.List;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;
import org.hibernate.spatial.FunctionKey;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.SPATIAL;

public class PostgisSqmFunctionDescriptors extends BaseSqmFunctionDescriptors {

	private final BasicTypeRegistry typeRegistry;

	public PostgisSqmFunctionDescriptors(FunctionContributions functionContributions) {
		super( functionContributions );
		typeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		addOperator("distance_2d", "<->");
		addOperator("distance_2d_bbox", "<#>");
		addOperator("distance_cpa", "|=|");
		addOperator( "distance_centroid_nd", "<<->>" );
		// <<#>> operator is apparently no longer supported?
		//addOperator( "distance_nd_bbox", "<<#>>" );
	}

	protected void addOperator(String name, String operator) {
		map.put(
				FunctionKey.apply( name ),
				new PostgisOperator(
						name,
						operator,
						new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), SPATIAL ),
						StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve(
								StandardBasicTypes.DOUBLE )
						)
				)
		);
	}

	static class PostgisOperator extends NamedSqmFunctionDescriptor {
		final private String operator;

		public PostgisOperator(
				String name,
				String op,
				ArgumentsValidator validator,
				FunctionReturnTypeResolver returnTypeResolver) {
			super( name, false, validator, returnTypeResolver );
			this.operator = op;
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			sqlAppender.appendSql( '(' );
			final Expression arg1 = (Expression) sqlAstArguments.get( 0 );
			walker.render( arg1, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.appendSql( operator );
			final Expression arg2 = (Expression) sqlAstArguments.get( 1 );
			walker.render( arg2, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.appendSql( ')' );
		}
	}
}
