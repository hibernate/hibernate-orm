/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.type.BasicPluralType;

import java.util.List;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base descriptor for `array_fill`, providing validation and type resolution
/// while subclasses implement database-specific rendering.
@SPI({ USE, IMPLEMENT })
public abstract class AbstractArrayFillFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	@SPI(IMPLEMENT)
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
