/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * The HQL {@code str()} function is now considered a synonym for {@code cast(x as String)}.
 *
 * @author Gavin King
 */
public class CastStrEmulation
		extends AbstractSqmFunctionDescriptor {

	public CastStrEmulation(TypeConfiguration typeConfiguration) {
		super(
				"str",
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				null
		);
	}

	protected CastStrEmulation(
			String name,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver, null );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final SqmTypedNode<?> argument = arguments.get( 0 );
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "cast" )
				.generateSqmExpression(
						asList(
								argument,
								new SqmCastTarget<>(
										queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
										queryEngine.getCriteriaBuilder()
								)
						),
						impliedResultType,
						queryEngine
				);
	}
}
