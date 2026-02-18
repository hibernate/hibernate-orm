/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

public class SpannerPostgreSQLRegexpLikeFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private static final String CAST = "::text";

	public SpannerPostgreSQLRegexpLikeFunction(TypeConfiguration typeConfiguration) {
		super( "regexp_like",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING,  STRING, STRING));
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
		sqlAppender.append( "regexp_match(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( "," );
		sqlAstArguments.get( 1 ).accept( walker );
		if (sqlAstArguments.size() > 2) {
			sqlAppender.append( "," );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.append( ") IS NOT NULL" );
	}
}
