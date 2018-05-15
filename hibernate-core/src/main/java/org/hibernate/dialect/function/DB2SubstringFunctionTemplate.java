/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * When "substring" function is used for DB2, this implementation of {@link SqmFunctionTemplate}
 * will render "substr" or "substring", depending on the last argument being used. If the last
 * argument is a string unit ("CODEUNITS16", "CODEUNITS32", or "OCTETS"), then the function
 * will be rendered as "substring"; otherwise, it will be rendered as "substr".
 * <p/>
 * ANSI SQL-92 standard defines "substring" without string units, which is more similar to DB2's "substr",
 * so it makes sense to use DB2's "substr" function when string units are not provided.
 * <p/>
 * Background: DB2 has both "substr" and "substring", which are different functions that are not
 * interchangeable. Prior to DB2 11.1, DB2's "substring" function requires an argument for string
 * units; without this argument, DB2 throws an exception. DB2's "substr" function throws an exception
 * if string unit is provided as an argument.
 *
 * @author Gail Badner
 */
public class DB2SubstringFunctionTemplate implements SqmFunctionTemplate {
	private static final Set<String> possibleStringUnits = new HashSet<>(
			Arrays.asList( "CODEUNITS16", "CODEUNITS32", "OCTETS" )
	);

	public DB2SubstringFunctionTemplate() {
	}

	protected String getRenderedName(List arguments) {
		final String lastArgument = (String) arguments.get( arguments.size() - 1 );
		if ( lastArgument != null && possibleStringUnits.contains( lastArgument.toUpperCase() ) ) {
			return "substring";
		}
		else {
			return "substr";
		}
	}

	@Override
	public SqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments, AllowableFunctionReturnType impliedResultType) {
		return new DB2SubstringFunction(
				getRenderedName( arguments ),
				StandardSpiBasicTypes.STRING, arguments.get( 1 ),
				arguments.get( 2 ),
				null
		);
	}

	public static class DB2SubstringFunction extends SqmSubstringFunction {

		String functionName;

		public DB2SubstringFunction(
				String functionName,
				BasicValuedExpressableType resultType,
				SqmExpression source, SqmExpression startPosition, SqmExpression length) {
			super( resultType, source, startPosition, length );
			this.functionName = functionName;
		}

		@Override
		public String getFunctionName() {
			return functionName;
		}
	}
}
