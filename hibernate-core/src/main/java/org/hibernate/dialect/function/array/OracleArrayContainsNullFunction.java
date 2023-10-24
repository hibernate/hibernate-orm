/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleArrayContainsNullFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public OracleArrayContainsNullFunction(TypeConfiguration typeConfiguration) {
		super(
				"array_contains_null",
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 1 ),
						ArrayArgumentValidator.DEFAULT_INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Boolean.class ) ),
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final String arrayTypeName = DdlTypeHelper.getTypeName( arrayExpression.getExpressionType(), walker );
		sqlAppender.appendSql( arrayTypeName );
		sqlAppender.append( "_position(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ",null)>0" );
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY array)";
	}
}
