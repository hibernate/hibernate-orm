/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.TrimSpec;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static java.util.Arrays.asList;

/**
 * A {@link SqmFunctionTemplate} implementation that emulates the ANSI SQL trim function
 * on dialects which do not support the full definition.  However, this function
 * definition does assume the availability of ltrim, rtrim, and replace functions
 * which it uses in various combinations to emulate the desired ANSI trim()
 * functionality.
 *
 * @author Steve Ebersole
 */
public class TransactSQLTrimEmulation implements SqmFunctionTemplate {
	/**
	 * The default {@code ltrim} function name
	 */
	public static final String LTRIM = "ltrim";

	/**
	 * The default {@code rtrim} function name
	 */
	public static final String RTRIM = "rtrim";

	/**
	 * The default {@code replace} function name
	 */
	public static final String REPLACE = "replace";

	/**
	 * The placeholder used to represent whitespace
	 */
	private static final String TRIM_CHAR_PLACEHOLDER = "${space}$";

	private final String ltrimFunctionName;
	private final String rtrimFunctionName;
	private final String replaceFunctionName;

	/**
	 * Constructs a new AnsiTrimEmulationFunction using {@link #LTRIM}, {@link #RTRIM}, and {@link #REPLACE}
	 * respectively.
	 *
	 * @see #TransactSQLTrimEmulation(String,String,String)
	 */
	public TransactSQLTrimEmulation() {
		this( LTRIM, RTRIM, REPLACE );
	}

	/**
	 * Constructs a <tt>trim()</tt> emulation function definition using the specified function calls.
	 *
	 * @param ltrimFunctionName The <tt>left trim</tt> function to use.
	 * @param rtrimFunctionName The <tt>right trim</tt> function to use.
	 * @param replaceFunctionName The <tt>replace</tt> function to use.
	 */
	public TransactSQLTrimEmulation(String ltrimFunctionName, String rtrimFunctionName, String replaceFunctionName) {
		this.ltrimFunctionName = ltrimFunctionName;
		this.rtrimFunctionName = rtrimFunctionName;
		this.replaceFunctionName = replaceFunctionName;
	}

	@Override
	public <T> SelfRenderingSqmFunction<T>  makeSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		final TrimSpec specification = ( (SqmTrimSpecification) arguments.get( 0 ) ).getSpecification();
		//noinspection unchecked
		final SqmLiteral<Character> trimCharacterExpr = (SqmLiteral<Character>) arguments.get( 1 );
		final SqmExpression sourceExpr = (SqmExpression) arguments.get( 1 );

		// NOTE we assume here that the specific ltrim/rtrim/replace function names do not need additional resolution
		//		against the registry!

		switch ( specification ) {
			case LEADING: {
				return trimLeading( trimCharacterExpr, sourceExpr, queryEngine );
			}
			case TRAILING: {
				return trimTrailing( trimCharacterExpr, sourceExpr, queryEngine );
			}
			default: {
				return trimBoth( trimCharacterExpr, sourceExpr, queryEngine );
			}
		}
	}

	private SelfRenderingSqmFunction trimLeading(
			SqmLiteral<Character> trimChar,
			SqmExpression source,
			QueryEngine queryEngine) {
		if ( trimChar.getLiteralValue() == ' ' ) {
			return trimLeadingSpaces( source, queryEngine );
		}
		else {
			return trimLeadingNonSpaces( trimChar, source, queryEngine );
		}
	}

	private SelfRenderingSqmFunction trimLeadingSpaces(SqmExpression source, QueryEngine queryEngine) {
		return ltrim( source, queryEngine );
	}

	private SelfRenderingSqmFunction trimLeadingNonSpaces(
			SqmLiteral<Character> trimChar,
			SqmExpression source,
			QueryEngine queryEngine) {
		final SqmLiteral<Character> space = charExpr( ' ', queryEngine );
		final SqmLiteral<String> placeholder = placeholder( queryEngine );

		// replace all the '${space}$' text with space chars
		return replace(
				// replace all space chars with the replacement char
				replace(
						// perform left-trimming
						ltrim(
								// replace all the actual replacement chars with space chars
								replace(
										// replace all space chars with the text '${space}$'
										replace(
												source,
												space,
												placeholder,
												queryEngine
										),
										space,
										placeholder,
										queryEngine
								),
								queryEngine
						),
						space,
						trimChar,
						queryEngine
				),
				placeholder,
				space,
				queryEngine
		);
	}

	private SelfRenderingSqmFunction trimTrailing(
			SqmLiteral<Character> trimChar,
			SqmExpression source,
			QueryEngine queryEngine) {
		if ( trimChar.getLiteralValue() == ' ' ) {
			return trimTrailingSpaces( source, queryEngine );
		}
		else {
			return trimTrailingNonSpaces( trimChar, source, queryEngine );
		}
	}

	private SelfRenderingSqmFunction trimTrailingSpaces(SqmExpression sourceExpr, QueryEngine queryEngine) {
		return rtrim( sourceExpr, queryEngine );
	}

	private SelfRenderingSqmFunction trimTrailingNonSpaces(
			SqmLiteral<Character> trimChar,
			SqmExpression source,
			QueryEngine queryEngine) {
		final SqmLiteral<Character> space = charExpr( ' ', queryEngine );
		final SqmLiteral<String> placeholder = placeholder( queryEngine );

		// replace all the '${space}$' text with space chars
		return replace(
				// replace all space chars with the replacement char
				replace(
						// perform right-trimming
						rtrim(
								// replace all the actual replacement chars with space chars
								replace(
										// replace all space chars with the text '${space}$'
										replace(
												source,
												space,
												placeholder,
												queryEngine
										),
										space,
										placeholder,
										queryEngine
								),
								queryEngine
						),
						space,
						trimChar,
						queryEngine
				),
				placeholder,
				space,
				queryEngine
		);
	}

	private SelfRenderingSqmFunction trimBoth(
			SqmLiteral<Character> trimCharacterExpr,
			SqmExpression sourceExpr,
			QueryEngine queryEngine) {
		// BOTH
		if ( trimCharacterExpr.getLiteralValue() == ' ' ) {
			return trimBothSpaces( sourceExpr, queryEngine );
		}
		else {
			return trimBothNonSpaces( trimCharacterExpr, sourceExpr, queryEngine );
		}
	}

	private SelfRenderingSqmFunction trimBothSpaces(SqmExpression sourceExpr, QueryEngine queryEngine) {
		return ltrim( rtrim( sourceExpr, queryEngine ), queryEngine );
	}

	private SelfRenderingSqmFunction trimBothNonSpaces(SqmLiteral<Character> trimChar, SqmExpression source, QueryEngine queryEngine) {
		final SqmLiteral<Character> space = charExpr( ' ', queryEngine );
		final SqmLiteral<String> placeholder = placeholder( queryEngine );

		// replace all the '${space}$' text with space chars
		return replace(
				// replace all space chars with the replacement char
				replace(
						// perform left-trimming (that removes any of the space chars we just added which occur at the beginning of the text)
						ltrim(
								// perform right-trimming
								rtrim(
										// replace all the actual replacement chars with space chars
										replace(
												// replace all space chars with the text '${space}$'
												replace(
														source,
														space,
														placeholder,
														queryEngine
												),
												space,
												placeholder,
												queryEngine
										),
										queryEngine
								),
								queryEngine
						),
						space,
						trimChar,
						queryEngine
				),
				placeholder,
				space,
				queryEngine
		);
	}

	private SelfRenderingSqmFunction replace(
			SqmExpression source,
			SqmExpression searchPattern,
			SqmExpression replacement,
			QueryEngine queryEngine) {
		return function(
				replaceFunctionName,
				queryEngine,
				source,
				searchPattern,
				replacement
		);
	}

	private SelfRenderingSqmFunction rtrim(SqmExpression source, QueryEngine queryEngine) {
		return function( rtrimFunctionName, queryEngine, source );
	}

	private SelfRenderingSqmFunction ltrim(SqmExpression source, QueryEngine queryEngine) {
		return function( ltrimFunctionName, queryEngine, source );
	}

	private static SelfRenderingSqmFunction function(
			String name,
			QueryEngine queryEngine,
			SqmExpression... arguments) {
		return queryEngine.getSqmFunctionRegistry().findFunctionTemplate(name).makeSqmFunctionExpression(
				asList( arguments ),
				StandardSpiBasicTypes.STRING,
				queryEngine
		);
	}

	private SqmLiteral<String> placeholder(QueryEngine queryEngine) {
		return new SqmLiteral<>( TRIM_CHAR_PLACEHOLDER, StandardSpiBasicTypes.STRING, queryEngine.getCriteriaBuilder() );
	}

	private SqmLiteral<Character> charExpr(char trimChar, QueryEngine queryEngine) {
		return new SqmLiteral<>( trimChar, StandardSpiBasicTypes.CHARACTER, queryEngine.getCriteriaBuilder() );
	}


}
