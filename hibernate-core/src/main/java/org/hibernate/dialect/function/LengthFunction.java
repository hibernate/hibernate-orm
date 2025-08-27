/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
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
 * A length function with separate patterns for string and clob argument.
 */
public class LengthFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final PatternRenderer stringPatternRenderer;
	private final PatternRenderer clobPatternRenderer;

	public LengthFunction(String functionName, String stringPattern, String clobPattern, TypeConfiguration typeConfiguration) {
		super(
				functionName,
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 1 ),
						FunctionParameterType.STRING_OR_CLOB
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING )
		);
		this.stringPatternRenderer = new PatternRenderer( stringPattern );
		this.clobPatternRenderer = new PatternRenderer( clobPattern );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression expression = (Expression) sqlAstArguments.get( 0 );
		final JdbcType jdbcType = expression.getExpressionType().getSingleJdbcMapping().getJdbcType();
		switch ( jdbcType.getDdlTypeCode() ) {
			case SqlTypes.CLOB:
			case SqlTypes.NCLOB:
				clobPatternRenderer.render( sqlAppender, sqlAstArguments, walker );
				break;
			default:
				stringPatternRenderer.render( sqlAppender, sqlAstArguments, walker );
				break;
		}
	}
}
