/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.BasicPluralType;

import java.util.List;

/**
 * Encapsulates the validator, return type and argument type resolvers for the array_contains function.
 * Subclasses only have to implement the rendering.
 */
public abstract class AbstractArrayFillFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractArrayFillFunction(boolean list) {
		super(
				"array_fill" + ( list ? "_list" : "" ),
				new ArgumentTypesValidator( null, FunctionParameterType.NO_UNTYPED, FunctionParameterType.INTEGER ),
				list
						? ArrayViaElementArgumentReturnTypeResolver.VARARGS_LIST_INSTANCE
						: ArrayViaElementArgumentReturnTypeResolver.VARARGS_INSTANCE,
				ArrayFillArgumentsValidator.INSTANCE
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(OBJECT element, INTEGER elementCount)";
	}

	private static class ArrayFillArgumentsValidator implements AbstractFunctionArgumentTypeResolver {

		public static final FunctionArgumentTypeResolver INSTANCE = new ArrayFillArgumentsValidator();

		private ArrayFillArgumentsValidator() {
		}

		@Override
		public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
			if ( argumentIndex == 0 ) {
				final MappingModelExpressible<?> impliedReturnType = converter.resolveFunctionImpliedReturnType();
				return impliedReturnType instanceof BasicPluralType<?, ?> basicPluralType
						? basicPluralType.getElementType()
						: null;
			}
			else {
				return converter.getCreationContext().getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( Integer.class );
			}
		}
	}

}
