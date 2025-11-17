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
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * DB2's substring() function requires a code unit and substr() can't optionally take it,
 * so we render substr() by default. If the code unit is passed, we render substring().
 */
public class DB2SubstringFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean needsCodeUnit;

	public DB2SubstringFunction(TypeConfiguration typeConfiguration) {
		this( true, typeConfiguration );
	}

	public DB2SubstringFunction(boolean needsCodeUnit, TypeConfiguration typeConfiguration) {
		super(
				"substring",
				new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 4 ), STRING, INTEGER, INTEGER, FunctionParameterType.ANY ),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.getBasicTypeRegistry().resolve(
						StandardBasicTypes.STRING ) ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, INTEGER, INTEGER )
		);
		this.needsCodeUnit = needsCodeUnit;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final int argumentCount = arguments.size();
		sqlAppender.appendSql( "substring(" );
		arguments.get( 0 ).accept( walker );
		for ( int i = 1; i < argumentCount; i++ ) {
			sqlAppender.appendSql( ',' );
			arguments.get( i ).accept( walker );
		}
		if ( argumentCount != 4 && needsCodeUnit ) {
			sqlAppender.appendSql( ",codeunits32" );
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	public String getSignature(String name) {
		return "(STRING string, INTEGER start[, INTEGER length[, units]])";
	}
}
