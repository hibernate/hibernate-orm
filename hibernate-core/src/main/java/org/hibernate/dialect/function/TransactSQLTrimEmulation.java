/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.type.StandardBasicTypes;

/**
 * A {@link SqmFunctionDescriptor} implementation that emulates the ANSI SQL trim function
 * on dialects which do not support the full definition.  However, this function
 * definition does assume the availability of ltrim, rtrim, and replace functions
 * which it uses in various combinations to emulate the desired ANSI trim()
 * functionality.
 *
 * @author Steve Ebersole
 */
public class TransactSQLTrimEmulation implements SqmFunctionDescriptor {
	private static final ArgumentsValidator ARGUMENTS_VALIDATOR = StandardArgumentsValidators.exactly( 3 );

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
		super();
		this.ltrimFunctionName = ltrimFunctionName;
		this.rtrimFunctionName = rtrimFunctionName;
		this.replaceFunctionName = replaceFunctionName;
	}

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		ARGUMENTS_VALIDATOR.validate( arguments );

		final TrimSpec specification = ( (SqmTrimSpecification) arguments.get( 0 ) ).getSpecification();
		//noinspection unchecked
		final SqmLiteral<Character> trimCharacterSqmExpr = (SqmLiteral<Character>) arguments.get( 1 );
		final SqmExpression sourceSqmExpr = (SqmExpression) arguments.get( 2 );

		// NOTE we assume here that the specific ltrim/rtrim/replace function names do not need additional resolution
		//		against the registry!

		final JdbcLiteral<Character> trimCharacterExpr = literal( trimCharacterSqmExpr.getLiteralValue() );
		final Expression sourceExpr = (Expression) sourceSqmExpr.accept( converter );

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

	private Expression trimLeading(JdbcLiteral<Character> trimChar, Expression source) {
		if ( trimChar.getLiteralValue().equals( ' ' ) ) {
			return trimLeadingSpaces( source );
		}
		else {
			return trimLeadingNonSpaces( trimChar, source );
		}
	}

	private Expression trimLeadingSpaces(Expression source) {
		return ltrim( source );
	}

	private Expression trimLeadingNonSpaces(
			JdbcLiteral<Character> trimChar,
			Expression source) {
		final JdbcLiteral<Character> space = literal( ' ' );
		final JdbcLiteral<String> placeholder = placeholder();

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

	private Expression trimTrailing(
			JdbcLiteral<Character> trimChar,
			Expression source) {
		if ( trimChar.getLiteralValue().equals( ' ' ) ) {
			return trimTrailingSpaces( source );
		}
		else {
			return trimTrailingNonSpaces( trimChar, source);
		}
	}

	private Expression trimTrailingSpaces(Expression sourceExpr) {
		return rtrim( sourceExpr );
	}

	private Expression trimTrailingNonSpaces(
			JdbcLiteral<Character> trimChar,
			Expression source) {
		final JdbcLiteral<Character> space = literal( ' ' );
		final JdbcLiteral<String> placeholder = placeholder();

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

	private Expression trimBoth(
			JdbcLiteral<Character> trimCharacterExpr,
			Expression sourceExpr) {
		// BOTH
		if ( trimCharacterExpr.getLiteralValue().equals( ' ' ) ) {
			return trimBothSpaces( sourceExpr );
		}
		else {
			return trimBothNonSpaces( trimCharacterExpr, sourceExpr );
		}
	}

	private Expression trimBothSpaces(Expression sourceExpr) {
		return ltrim( rtrim( sourceExpr ) );
	}

	private Expression trimBothNonSpaces(
			JdbcLiteral<Character> trimChar,
			Expression source) {
		final JdbcLiteral<Character> space = literal( ' ' );
		final JdbcLiteral<String> placeholder = placeholder();

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

	private Expression replace(
			Expression source,
			Expression searchPattern,
			Expression replacement) {
		return function(
				replaceFunctionName,
				source,
				searchPattern,
				replacement
		);
	}

	private Expression rtrim(Expression source) {
		return function( rtrimFunctionName, source );
	}

	private Expression ltrim(Expression source) {
		return function( ltrimFunctionName, source );
	}

	private static Expression function(
			String name,
			Expression... arguments) {
		return new SelfRenderingExpression() {
			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstWalker walker,
					SessionFactoryImplementor sessionFactory) {
				sqlAppender.appendSql( name );
				sqlAppender.appendSql( "( " );
				arguments[0].accept( walker );

				for ( int i = 1; i < arguments.length; i++ ) {
					sqlAppender.appendSql( ", " );
					arguments[i].accept( walker );
				}
			}

			@Override
			public MappingModelExpressable getExpressionType() {
				return StandardBasicTypes.STRING;
			}
		};
	}

	private JdbcLiteral<String> placeholder() {
		return new JdbcLiteral<>( TRIM_CHAR_PLACEHOLDER, StandardBasicTypes.STRING );
	}

	private JdbcLiteral<Character> literal(char trimChar) {
		return new JdbcLiteral<>( trimChar, StandardBasicTypes.CHARACTER );
	}


}
