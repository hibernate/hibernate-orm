/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * Emulates the ANSI SQL-standard {@code position()} function using {@code locate()}.
 *
 * @author Gavin King
 */
public class LocatePositionEmulation extends AbstractSqmFunctionDescriptor {

	public LocatePositionEmulation(TypeConfiguration typeConfiguration) {
		super(
				"position",
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), STRING, STRING ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, STRING )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "locate" )
				.generateSqmExpression( arguments, impliedResultType, queryEngine);
	}

	@Override
	public String getArgumentListSignature() {
		return "(STRING pattern in STRING string)";
	}
}
