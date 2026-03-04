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

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

public class SpannerPostgreSQLLocateFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public SpannerPostgreSQLLocateFunction(TypeConfiguration typeConfiguration) {
		super( "locate",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING,  STRING, INTEGER));
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
		var argumentCount = sqlAstArguments.size();
		sqlAppender.append( "strpos(" );
		if ( argumentCount == 3 ) {
			sqlAppender.appendSql( "substr(" );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ", " );
			sqlAstArguments.get( 2 ).accept( walker );
			sqlAppender.appendSql( ")" );
		}
		else {
			sqlAstArguments.get( 1 ).accept( walker );
		}
		sqlAppender.append( "," );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.append( ")" );
	}
}
