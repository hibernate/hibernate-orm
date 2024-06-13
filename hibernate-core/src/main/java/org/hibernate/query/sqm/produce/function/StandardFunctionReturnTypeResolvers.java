/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.sql.Types;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.NullSqmExpressible;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.type.SqlTypes.isCharacterOrClobType;
import static org.hibernate.type.SqlTypes.isNumericType;

/**
 * @author Steve Ebersole
 */
public class StandardFunctionReturnTypeResolvers {
	/**
	 * Disallow instantiation
	 */
	private StandardFunctionReturnTypeResolvers() {
	}

	/**
	 * A resolver that defines an invariant result type.  E.g. `substring` always
	 * returns a String.  Note however that to account for attribute converters and
	 * such, this resolver allows the context-implied expression type to be the
	 * return type so long as the Java types are compatible.
	 */
	public static FunctionReturnTypeResolver invariant(BasicType<?> invariantType) {
		if ( invariantType == null ) {
			throw new IllegalArgumentException( "Passed `invariantType` for function return cannot be null" );
		}

		return new FunctionReturnTypeResolver() {
			@Override
			public ReturnableType<?> resolveFunctionReturnType(
					ReturnableType<?> impliedType,
					@Nullable SqmToSqlAstConverter converter,
					List<? extends SqmTypedNode<?>> arguments,
					TypeConfiguration typeConfiguration) {
				return isAssignableTo( invariantType, impliedType ) ? impliedType : invariantType;
			}

			@Override
			public BasicValuedMapping resolveFunctionReturnType(
					Supplier<BasicValuedMapping> impliedTypeAccess,
					List<? extends SqlAstNode> arguments) {
				return useImpliedTypeIfPossible( invariantType, impliedTypeAccess.get() );
			}

			@Override
			public String getReturnType() {
				return invariantType.getJavaType().getSimpleName();
			}
		};
	}

	public static FunctionReturnTypeResolver useArgType(int argPosition) {
		return new FunctionReturnTypeResolver() {
			@Override
			public ReturnableType<?> resolveFunctionReturnType(
					ReturnableType<?> impliedType,
					@Nullable SqmToSqlAstConverter converter,
					List<? extends SqmTypedNode<?>> arguments,
					TypeConfiguration typeConfiguration) {
				ReturnableType<?> argType = extractArgumentType( arguments, argPosition );
				return isAssignableTo( argType, impliedType ) ? impliedType : argType;
			}

			@Override
			public BasicValuedMapping resolveFunctionReturnType(
					Supplier<BasicValuedMapping> impliedTypeAccess,
					List<? extends SqlAstNode> arguments) {
				final BasicValuedMapping specifiedArgType = extractArgumentValuedMapping( arguments, argPosition );
				return useImpliedTypeIfPossible( specifiedArgType, impliedTypeAccess.get() );
			}
		};
	}

	public static FunctionReturnTypeResolver useFirstNonNull() {
		return new FunctionReturnTypeResolver() {
			@Override
			public BasicValuedMapping resolveFunctionReturnType(
					Supplier<BasicValuedMapping> impliedTypeAccess,
					List<? extends SqlAstNode> arguments) {
				for ( SqlAstNode arg: arguments ) {
					if ( ! ( arg instanceof Expression ) ) {
						continue;
					}

					final JdbcMappingContainer nodeType = ( (Expression) arg ).getExpressionType();
					if ( nodeType instanceof BasicValuedMapping ) {
						final BasicValuedMapping argType = (BasicValuedMapping) nodeType;
						return useImpliedTypeIfPossible( argType, impliedTypeAccess.get() );
					}
				}

				return impliedTypeAccess.get();
			}

			@Override
			public ReturnableType<?> resolveFunctionReturnType(
					ReturnableType<?> impliedType,
					@Nullable SqmToSqlAstConverter converter,
					List<? extends SqmTypedNode<?>> arguments,
					TypeConfiguration typeConfiguration) {
				for ( int i = 0; i < arguments.size(); i++ ) {
					if ( arguments.get( i ) != null ) {
						final ReturnableType<?> argType = extractArgumentType( arguments, i + 1 );
						if ( argType != null ) {
							return isAssignableTo( argType, impliedType ) ? impliedType : argType;
						}
					}
				}
				return impliedType;
			}
		};
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Internal helpers

	@Internal
	public static boolean isAssignableTo(ReturnableType<?> defined, ReturnableType<?> implied) {
		if ( implied == null ) {
			return false;
		}

		if ( defined == null ) {
			return true;
		}

		if ( !( implied instanceof BasicType ) || !( defined instanceof BasicType ) ) {
			return false;
		}
		return isAssignableTo(
				( (BasicType<?>) defined ).getJdbcMapping(),
				( (BasicType<?>) implied ).getJdbcMapping()
		);
	}

	@Internal
	public static boolean isAssignableTo(JdbcMapping defined, JdbcMapping implied) {
		//This list of cases defines legal promotions from a SQL function return
		//type specified in the function template (i.e. in the Dialect) and a type
		//that is determined by how the function is used in the HQL query. In essence
		//the types are compatible if the map to the same JDBC type, of if they are
		//both numeric types.
		int impliedTypeCode = implied.getJdbcType().getDefaultSqlTypeCode();
		int definedTypeCode = defined.getJdbcType().getDefaultSqlTypeCode();
		return impliedTypeCode == definedTypeCode
				|| isNumericType( impliedTypeCode ) && isNumericType( definedTypeCode )
				|| isCharacterOrClobType( impliedTypeCode ) && isCharacterOrClobType( definedTypeCode );
	}

	@Internal
	public static BasicValuedMapping useImpliedTypeIfPossible(
			BasicValuedMapping defined,
			BasicValuedMapping implied) {
		if ( defined == null ) {
			return implied;
		}

		if ( implied == null ) {
			return defined;
		}

		return areCompatible( defined, implied ) ? implied : defined;
	}

	private static boolean areCompatible(
			BasicValuedMapping defined,
			BasicValuedMapping implied) {
		if ( defined == null || implied == null) {
			return true;
		}

		if ( defined.getJdbcMapping() == null ) {
			return true;
		}

		if ( implied.getJdbcMapping() == null ) {
			return true;
		}

		//This list of cases defines legal promotions from a SQL function return
		//type specified in the function template (i.e. in the Dialect) and a type
		//that is determined by how the function is used in the HQL query. In essence
		//the types are compatible if the map to the same JDBC type, of if they are
		//both numeric types.
		return isAssignableTo( defined.getJdbcMapping(), implied.getJdbcMapping() );
	}

	public static ReturnableType<?> extractArgumentType(
			List<? extends SqmTypedNode<?>> arguments,
			int position) {
		final SqmTypedNode<?> specifiedArgument = arguments.get( position - 1 );
		final SqmExpressible<?> specifiedArgType = getArgumentExpressible( specifiedArgument );
		if ( specifiedArgType == null || specifiedArgType instanceof NullSqmExpressible ) {
			return null;
		}
		else if ( !(specifiedArgType instanceof ReturnableType) ) {
			throw new FunctionArgumentException(
					String.format(
							Locale.ROOT,
							"Function argument [%s] of type [%s] at specified position [%d] in call arguments was not typed as an allowable function return type",
							specifiedArgument,
							specifiedArgType,
							position
					)
			);
		}
		else {
			return (ReturnableType<?>) specifiedArgType;
		}
	}

	private static SqmExpressible<?> getArgumentExpressible(SqmTypedNode<?> specifiedArgument) {
		final SqmExpressible<?> expressible = specifiedArgument.getNodeType();
		final SqmExpressible<?> specifiedArgType = expressible instanceof SqmTypedNode<?>
				? ( (SqmTypedNode<?>) expressible ).getNodeType()
				: expressible;
		return specifiedArgType instanceof SqmPathSource
				? ( (SqmPathSource<?>) specifiedArgType ).getSqmPathType()
				: specifiedArgType;
	}

	public static JdbcMapping extractArgumentJdbcMapping(
			TypeConfiguration typeConfiguration,
			List<? extends SqmTypedNode<?>> arguments,
			int position) {
		final SqmTypedNode<?> specifiedArgument = arguments.get( position - 1 );
		final SqmExpressible<?> specifiedArgType = specifiedArgument.getNodeType();
		if ( specifiedArgType instanceof BasicType<?> ) {
			return ( (BasicType<?>) specifiedArgType ).getJdbcMapping();
		}
		else {
			final BasicType<?> basicType = typeConfiguration.getBasicTypeForJavaType(
					specifiedArgType.getExpressibleJavaType().getJavaTypeClass()
			);
			if ( basicType == null ) {
				throw new FunctionArgumentException(
						String.format(
								Locale.ROOT,
								"Function argument [%s] of type [%s] at specified position [%d] in call arguments was not typed as basic type",
								specifiedArgument,
								specifiedArgType,
								position
						)
				);
			}

			return basicType.getJdbcMapping();
		}
	}

	public static BasicValuedMapping extractArgumentValuedMapping(List<? extends SqlAstNode> arguments, int position) {
		final SqlAstNode specifiedArgument = arguments.get( position-1 );
		final JdbcMappingContainer specifiedArgType = specifiedArgument instanceof Expression
				? ( (Expression) specifiedArgument ).getExpressionType()
				: null;

		if ( specifiedArgType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) specifiedArgType;
		}

		throw new FunctionArgumentException(
				String.format(
						Locale.ROOT,
						"Function argument [%s] at specified position [%d] in call arguments was not typed as an allowable function return type",
						specifiedArgument,
						position
				)
		);
	}
}
