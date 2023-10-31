/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.internal.TypecheckUtil;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BottomType;
import org.hibernate.type.spi.TypeConfiguration;

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
				TypeConfiguration typeConfiguration) {
			final SessionFactoryImplementor sessionFactory = typeConfiguration.getSessionFactory();
			final int size = arguments.size();
			SqmExpressible<?> firstType = null;
			for ( int i = 0; i < size; i++ ) {
				final SqmExpressible<?> argument = arguments.get( i ).getExpressible();
				if ( firstType == null ) {
					firstType = argument;
				}
				else if ( !TypecheckUtil.areTypesComparable( firstType, argument, sessionFactory ) ) {
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
					else if ( firstType != argumentType ) {
						throw new FunctionArgumentException(
								String.format(
										"All array arguments must have a type compatible to the first argument type [%s], but argument %d has type '%s'",
										firstType,
										i + 1,
										argumentType
								)
						);
					}
				}
			}
		}
	}

}
