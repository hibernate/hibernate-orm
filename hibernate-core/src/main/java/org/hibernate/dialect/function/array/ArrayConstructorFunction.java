/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BottomType;

import static org.hibernate.query.sqm.internal.TypecheckUtil.areTypesComparable;

public class ArrayConstructorFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean withKeyword;

	public ArrayConstructorFunction(boolean list, boolean withKeyword) {
		super(
				"array" + ( list ? "_list" : "" ),
				ArrayConstructorArgumentsValidator.INSTANCE,
				list
						? ArrayViaElementArgumentReturnTypeResolver.VARARGS_LIST_INSTANCE
						: ArrayViaElementArgumentReturnTypeResolver.VARARGS_INSTANCE,
				StandardFunctionArgumentTypeResolvers.NULL
		);
		this.withKeyword = withKeyword;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( withKeyword ) {
			sqlAppender.append( "array" );
		}
		final int size = arguments.size();
		if ( size == 0 ) {
			sqlAppender.append( '[' );
		}
		else {
			char separator = '[';
			for ( int i = 0; i < size; i++ ) {
				SqlAstNode argument = arguments.get( i );
				sqlAppender.append( separator );
				argument.accept( walker );
				separator = ',';
			}
		}
		sqlAppender.append( ']' );
	}

	private static class ArrayConstructorArgumentsValidator implements ArgumentsValidator {

		public static final ArgumentsValidator INSTANCE = new ArrayConstructorArgumentsValidator();

		private ArrayConstructorArgumentsValidator() {
		}

		@Override
		public void validate(
				List<? extends SqmTypedNode<?>> arguments,
				String functionName,
				BindingContext bindingContext) {
			final int size = arguments.size();
			SqmBindableType<?> firstType = null;
			for ( int i = 0; i < size; i++ ) {
				final SqmBindableType<?> argument = arguments.get( i ).getExpressible();
				if ( firstType == null ) {
					firstType = argument;
				}
				else if ( !areTypesComparable( firstType, argument, bindingContext ) ) {
					throw new FunctionArgumentException(
							String.format(
									"All array arguments must have a compatible type compatible to the first argument type [%s], but argument %d has type '%s'",
									firstType.getTypeName(),
									i + 1,
									argument.getTypeName()
							)
					);
				}
			}
		}

		@Override
		public void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {
			final int size = arguments.size();
			JdbcMappingContainer firstType = null;
			for ( int i = 0; i < size; i++ ) {
				final JdbcMappingContainer argumentType = ( (Expression) arguments.get( i ) ).getExpressionType();
				if ( argumentType != null && !( argumentType instanceof BottomType ) ) {
					if ( firstType == null ) {
						firstType = argumentType;
					}
					else if ( firstType.getSingleJdbcMapping() != argumentType.getSingleJdbcMapping() ) {
						throw new FunctionArgumentException(
								String.format(
										"All array arguments must have a type compatible to the first argument type [%s], but argument %d has type '%s'",
										firstType.getSingleJdbcMapping(),
										i + 1,
										argumentType.getSingleJdbcMapping()
								)
						);
					}
				}
			}
		}
	}

}
