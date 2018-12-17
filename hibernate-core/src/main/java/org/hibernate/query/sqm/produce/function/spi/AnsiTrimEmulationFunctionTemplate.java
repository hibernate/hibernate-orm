/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.Arrays;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.spi.TrimSpecificationExpressionWrapper;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.sql.ast.tree.spi.TrimSpecification;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * A {@link SqmFunctionTemplate} implementation that emulates the ANSI SQL trim function
 * on dialects which do not support the full definition.  However, this function
 * definition does assume the availability of ltrim, rtrim, and replace functions
 * which it uses in various combinations to emulate the desired ANSI trim()
 * functionality.
 *
 * @author Steve Ebersole
 */
public class AnsiTrimEmulationFunctionTemplate implements SqmFunctionTemplate {
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
	public static final String TRIM_CHAR_PLACEHOLDER = "${space}$";

	private final String ltrimFunctionName;
	private final String rtrimFunctionName;
	private final String replaceFunctionName;

	/**
	 * Constructs a new AnsiTrimEmulationFunction using {@link #LTRIM}, {@link #RTRIM}, and {@link #REPLACE}
	 * respectively.
	 *
	 * @see #AnsiTrimEmulationFunctionTemplate(String,String,String)
	 */
	public AnsiTrimEmulationFunctionTemplate() {
		this( LTRIM, RTRIM, REPLACE );
	}

	/**
	 * Constructs a <tt>trim()</tt> emulation function definition using the specified function calls.
	 *
	 * @param ltrimFunctionName The <tt>left trim</tt> function to use.
	 * @param rtrimFunctionName The <tt>right trim</tt> function to use.
	 * @param replaceFunctionName The <tt>replace</tt> function to use.
	 */
	public AnsiTrimEmulationFunctionTemplate(String ltrimFunctionName, String rtrimFunctionName, String replaceFunctionName) {
		this.ltrimFunctionName = ltrimFunctionName;
		this.rtrimFunctionName = rtrimFunctionName;
		this.replaceFunctionName = replaceFunctionName;
	}

	@Override
	public SqmFunction makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		final TrimSpecification specification = ( (TrimSpecificationExpressionWrapper) arguments.get( 0 ) ).getSpecification();
		//noinspection unchecked
		final SqmLiteral<Character> trimCharacterExpr = (SqmLiteral<Character>) arguments.get( 1 );
		final SqmExpression sourceExpr = arguments.get( 1 );

		// NOTE we assume here that the specific ltrim/rtrim/replace function names do not need additional resolution
		//		against the registry!

		switch ( specification ) {
			case LEADING: {
				return trimLeading( trimCharacterExpr, sourceExpr );
			}
			case TRAILING: {
				return trimTrailing( trimCharacterExpr, sourceExpr );
			}
			default: {
				return trimBoth( trimCharacterExpr, sourceExpr );
			}
		}
	}

	private SqmFunction trimLeading(
			SqmLiteral<Character> trimChar,
			SqmExpression source) {
		if ( trimChar.getLiteralValue() == ' ' ) {
			return trimLeadingSpaces( source );
		}
		else {
			return trimLeadingNonSpaces( trimChar, source );
		}
	}

	private SqmFunction trimLeadingSpaces(SqmExpression source) {
		return ltrim( source );
	}

	private SqmFunction trimLeadingNonSpaces(SqmLiteral<Character> trimChar, SqmExpression source) {
		final SqmLiteral<Character> space = charExpr( ' ' );
		final SqmLiteral<String> placeholder = placeholder();

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
												placeholder
										),
										space,
										placeholder
								)
						),
						space,
						trimChar
				),
				placeholder,
				space
		);
	}

	private SqmFunction trimTrailing(
			SqmLiteral<Character> trimChar,
			SqmExpression source) {
		if ( trimChar.getLiteralValue() == ' ' ) {
			return trimTrailingSpaces( source );
		}
		else {
			return trimTrailingNonSpaces( trimChar, source );
		}
	}

	private SqmFunction trimTrailingSpaces(SqmExpression sourceExpr) {
		return rtrim( sourceExpr );
	}

	private SqmFunction trimTrailingNonSpaces(
			SqmLiteral<Character> trimChar,
			SqmExpression source) {
		final SqmLiteral<Character> space = charExpr( ' ' );
		final SqmLiteral<String> placeholder = placeholder();

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
												placeholder
										),
										space,
										placeholder
								)
						),
						space,
						trimChar
				),
				placeholder,
				space
		);
	}

	private SqmFunction trimBoth(
			SqmLiteral<Character> trimCharacterExpr,
			SqmExpression sourceExpr) {
		// BOTH
		if ( trimCharacterExpr.getLiteralValue() == ' ' ) {
			return trimBothSpaces( sourceExpr );
		}
		else {
			return trimBothNonSpaces( trimCharacterExpr, sourceExpr );
		}
	}

	private SqmFunction trimBothSpaces(SqmExpression sourceExpr) {
		return ltrim( rtrim( sourceExpr ) );
	}

	private SqmFunction trimBothNonSpaces(SqmLiteral<Character> trimChar, SqmExpression source) {
		final SqmLiteral<Character> space = charExpr( ' ' );
		final SqmLiteral<String> placeholder = placeholder();

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
														placeholder
												),
												space,
												placeholder
										)
								)
						),
						space,
						trimChar
				),
				placeholder,
				space
		);
	}

	protected SqmFunction replace(SqmExpression source, SqmExpression searchPattern, SqmExpression replacement) {
		return function(
				replaceFunctionName,
				source,
				searchPattern,
				replacement
		);
	}

	protected SqmFunction rtrim(SqmExpression source) {
		return function( rtrimFunctionName, source );
	}

	protected SqmFunction ltrim(SqmExpression source) {
		return function( ltrimFunctionName, source );
	}

	private static SqmGenericFunction function(String name, SqmExpression... arguments) {
		return new SqmGenericFunction(
				name,
				StandardSpiBasicTypes.STRING,
				Arrays.asList( arguments )
		);
	}

	protected final SqmLiteral<String> placeholder() {
		return new SqmLiteral<>( TRIM_CHAR_PLACEHOLDER, StandardSpiBasicTypes.STRING );
	}

	protected SqmLiteral<Character> charExpr(char trimChar) {
		return new SqmLiteral<>( trimChar, StandardSpiBasicTypes.CHARACTER );
	}


}
