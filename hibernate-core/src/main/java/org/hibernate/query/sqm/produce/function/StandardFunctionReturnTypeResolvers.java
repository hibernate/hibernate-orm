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

import org.hibernate.QueryException;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

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
	 * such, this resolver allows the context-impled expression type to be the
	 * return type so long as the Java types are compatible.
	 */
	public static FunctionReturnTypeResolver invariant(BasicValuedMapping invariantType) {
		if ( invariantType == null ) {
			throw new IllegalArgumentException( "Passed `invariantType` for function return cannot be null" );
		}

		return new FunctionReturnTypeResolver() {
			@Override
			public BasicValuedMapping resolveFunctionReturnType(Supplier<BasicValuedMapping> impliedTypeAccess, List<? extends SqlAstNode> arguments) {
				return useImpliedTypeIfPossible(invariantType, impliedTypeAccess.get());
			}

			@Override
			public String getReturnType() {
				return invariantType.getBasicType().getJavaType().getSimpleName();
			}
		};
	}

	public static FunctionReturnTypeResolver useArgType(int argPosition) {
		return (impliedTypeAccess, arguments) -> {
			final BasicValuedMapping specifiedArgType = extractArgumentType( arguments, argPosition );
			return useImpliedTypeIfPossible( specifiedArgType, impliedTypeAccess.get() );
		};
	}

	public static FunctionReturnTypeResolver useFirstNonNull() {
		return (impliedTypeAccess, arguments) -> {
			for ( SqlAstNode arg: arguments ) {
				if ( ! ( arg instanceof Expression ) ) {
					continue;
				}

				final MappingModelExpressable nodeType = ( (Expression) arg ).getExpressionType();
				if ( nodeType instanceof BasicValuedMapping ) {
					final BasicValuedMapping argType = (BasicValuedMapping) nodeType;
					return useImpliedTypeIfPossible( argType, impliedTypeAccess.get() );
				}
			}

			return impliedTypeAccess.get();
		};
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Internal helpers

	private static BasicValuedMapping useImpliedTypeIfPossible(
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

	@SuppressWarnings({"SimplifiableIfStatement"})
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
		int impliedTypeCode = implied.getJdbcMapping().getSqlTypeDescriptor().getJdbcTypeCode();
		int definedTypeCode = defined.getJdbcMapping().getSqlTypeDescriptor().getJdbcTypeCode();
		return impliedTypeCode == definedTypeCode
			|| isInteger(impliedTypeCode) && isInteger(definedTypeCode)
			|| isFloat(impliedTypeCode) && isFloat(definedTypeCode);

	}

	private static boolean isInteger(int type) {
		return type == Types.INTEGER
			|| type == Types.BIGINT
			|| type == Types.SMALLINT
			|| type == Types.TINYINT;
	}

	private static boolean isFloat(int type) {
		return type == Types.FLOAT || type == Types.DOUBLE;
	}

	private static BasicValuedMapping extractArgumentType(List<? extends SqlAstNode> arguments, int position) {
		final SqlAstNode specifiedArgument = arguments.get( position-1 );
		final MappingModelExpressable specifiedArgType = specifiedArgument instanceof Expression
				? ( (Expression) specifiedArgument ).getExpressionType()
				: null;

		if ( specifiedArgType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) specifiedArgType;
		}

		throw new QueryException(
				String.format(
						Locale.ROOT,
						"Function argument [%s] at specified position [%d] in call arguments was not typed as an allowable function return type",
						specifiedArgument,
						position
				)
		);
	}
}
