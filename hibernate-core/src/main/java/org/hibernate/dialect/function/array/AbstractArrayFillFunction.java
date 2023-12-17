/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.type.BasicPluralType;

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

	private static class ArrayFillArgumentsValidator implements FunctionArgumentTypeResolver {

		public static final FunctionArgumentTypeResolver INSTANCE = new ArrayFillArgumentsValidator();

		private ArrayFillArgumentsValidator() {
		}

		@Override
		public MappingModelExpressible<?> resolveFunctionArgumentType(
				SqmFunction<?> function,
				int argumentIndex,
				SqmToSqlAstConverter converter) {
			if ( argumentIndex == 0 ) {
				final MappingModelExpressible<?> impliedReturnType = converter.resolveFunctionImpliedReturnType();
				return impliedReturnType instanceof BasicPluralType<?, ?>
						? ( (BasicPluralType<?, ?>) impliedReturnType ).getElementType()
						: null;
			}
			else {
				return converter.getCreationContext().getSessionFactory().getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( Integer.class );
			}
		}
	}

}
