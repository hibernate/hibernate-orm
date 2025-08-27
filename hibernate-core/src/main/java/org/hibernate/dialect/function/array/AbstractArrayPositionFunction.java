/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_position functions.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayPositionFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayPositionFunction(TypeConfiguration typeConfiguration) {
		super(
				"array_position",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.between( 2, 3 ),
								ArrayAndElementArgumentValidator.DEFAULT_INSTANCE
						),
						FunctionParameterType.ANY,
						FunctionParameterType.ANY,
						FunctionParameterType.INTEGER
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Integer.class ) ),
				new AbstractFunctionArgumentTypeResolver() {
					@Override
					public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
						if ( argumentIndex == 2 ) {
							return converter.getCreationContext()
									.getTypeConfiguration()
									.standardBasicTypeForJavaType( Integer.class );
						}
						else {
							return ArrayAndElementArgumentTypeResolver.DEFAULT_INSTANCE.resolveFunctionArgumentType(
									arguments,
									argumentIndex,
									converter
							);
						}
					}
				}
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY array, OBJECT element[, INTEGER startPosition])";
	}
}
