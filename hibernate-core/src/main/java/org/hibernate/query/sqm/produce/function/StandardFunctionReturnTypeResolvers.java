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

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.QueryException;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.StandardBasicTypes;

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
	public static FunctionReturnTypeResolver invariant(StandardBasicTypes.StandardBasicType<?> invariantType) {
		if ( invariantType == null ) {
			throw new IllegalArgumentException( "Passed `invariantType` for function return cannot be null" );
		}

		return (impliedType, arguments, typeConfiguration)
				-> isAssignableTo( invariantType, impliedType )
				? impliedType
				: typeConfiguration.resolveStandardBasicType( invariantType );
	}

	public static FunctionReturnTypeResolver useArgType(int argPosition) {
		return (impliedType, arguments, typeConfiguration) -> {
			AllowableFunctionReturnType<?> argType = extractArgumentType( arguments, argPosition );
			return isAssignableTo( argType, impliedType ) ? impliedType : argType;
		};
	}

	public static FunctionReturnTypeResolver useFirstNonNull() {
		return (impliedType, arguments, typeConfiguration) -> {
			for (SqmTypedNode<?> arg: arguments) {
				if (arg!=null && arg.getExpressableType() instanceof AllowableFunctionReturnType) {
					AllowableFunctionReturnType<?> argType = (AllowableFunctionReturnType<?>) arg.getExpressableType();
					return isAssignableTo( argType, impliedType ) ? impliedType : argType;
				}
			}
			return impliedType;
		};
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Internal helpers

	@SuppressWarnings({"unchecked", "SimplifiableIfStatement"})
	private static boolean isAssignableTo(
			AllowableFunctionReturnType<?> defined, AllowableFunctionReturnType<?> implied) {
		if ( implied == null || implied.getSqlExpressableType() == null ) {
			return false;
		}

		if ( defined == null || defined.getSqlExpressableType() == null ) {
			return true;
		}

		//This list of cases defines legal promotions from a SQL function return
		//type specified in the function template (i.e. in the Dialect) and a type
		//that is determined by how the function is used in the HQL query. In essence
		//the types are compatible if the map to the same JDBC type, of if they are
		//both numeric types.
		int impliedTypeCode = implied.getSqlExpressableType().getSqlTypeDescriptor().getJdbcTypeCode();
		int definedTypeCode = defined.getSqlExpressableType().getSqlTypeDescriptor().getJdbcTypeCode();
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

	private static AllowableFunctionReturnType extractArgumentType(List<SqmTypedNode<?>> arguments, int position) {
		final SqmTypedNode specifiedArgument = arguments.get( position-1 );
		final ExpressableType specifiedArgType = specifiedArgument.getExpressableType();
		if ( !(specifiedArgType instanceof AllowableFunctionReturnType) ) {
			throw new QueryException(
					String.format(
							Locale.ROOT,
							"Function argument [%s] at specified position [%d] in call arguments was not typed as an allowable function return type",
							specifiedArgType,
							position
					)
			);
		}

		return (AllowableFunctionReturnType) specifiedArgType;
	}
}
