/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * A concat function with a pattern for clob arguments.
 */
public class ConcatPipeFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final PatternRenderer clobPatternRenderer;

	public ConcatPipeFunction(String clobPattern, TypeConfiguration typeConfiguration) {
		super(
				"concat",
				StandardArgumentsValidators.min( 1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.impliedOrInvariant( typeConfiguration, STRING )
		);
		this.clobPatternRenderer = new PatternRenderer( clobPattern );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		String separator = "(";
		for ( int i = 0; i < sqlAstArguments.size(); i++ ) {
			final Expression expression = (Expression) sqlAstArguments.get( i );
			final JdbcType jdbcType = expression.getExpressionType().getSingleJdbcMapping().getJdbcType();
			sqlAppender.appendSql( separator );
			switch ( jdbcType.getDdlTypeCode() ) {
				case SqlTypes.CLOB:
				case SqlTypes.NCLOB:
					clobPatternRenderer.render( sqlAppender, Collections.singletonList( expression ), walker );
					break;
				default:
					expression.accept( walker );
					break;
			}
			separator = "||";
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	public String getSignature(String name) {
		return "(STRING string0[, STRING string1[, ...]])";
	}
}
