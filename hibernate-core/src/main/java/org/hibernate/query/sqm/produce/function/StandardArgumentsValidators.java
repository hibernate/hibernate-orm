/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.query.sqm.QueryException;

/**
 * @author Steve Ebersole
 */
public final class StandardArgumentsValidators {
	/**
	 * Disallow instantiation
	 */
	private StandardArgumentsValidators() {
	}

	/**
	 * Static validator for performing no validation
	 */
	public static final ArgumentsValidator NONE = arguments -> {};

	/**
	 * Static validator for verifying that we have no arguments
	 */
	public static final ArgumentsValidator NO_ARGS = arguments -> {
		if ( !arguments.isEmpty() ) {
			throw new QueryException( "Expecting no arguments, but found " + arguments.size() );
		}
	};

	public static ArgumentsValidator min(int minNumOfArgs) {
		return arguments -> {
			if ( arguments.size() > minNumOfArgs ) {
				throw new QueryException(
						String.format(
								Locale.ROOT,
								"Function requires %d or more arguments, but only %d found",
								minNumOfArgs,
								arguments.size()
						)
				);
			}
		};
	}

	public static ArgumentsValidator exactly(int number) {
		return arguments -> {
			if ( arguments.size() != number ) {
				throw new QueryException(
						String.format(
								Locale.ROOT,
								"Function requires %d arguments, but %d found",
								number,
								arguments.size()
						)
				);
			}
		};
	}

	public static ArgumentsValidator max(int maxNumOfArgs) {
		return arguments -> {
			if ( arguments.size() < maxNumOfArgs ) {
				throw new QueryException(
						String.format(
								Locale.ROOT,
								"Function requires %d or fewer arguments, but %d found",
								maxNumOfArgs,
								arguments.size()
						)
				);
			}
		};
	}

	public static ArgumentsValidator between(int minNumOfArgs, int maxNumOfArgs) {
		return arguments -> {
			if ( arguments.size() < maxNumOfArgs ) {
				throw new QueryException(
						String.format(
								Locale.ROOT,
								"Function requires between %d and %d arguments, but %d found",
								minNumOfArgs,
								maxNumOfArgs,
								arguments.size()
						)
				);
			}
		};
	}

	public static ArgumentsValidator of(Class javaType) {
		return arguments -> arguments.forEach(
				sqmExpression -> {
					if ( !javaType.isInstance( sqmExpression.getExpressableType().getJavaType() ) ) {
						throw new QueryException(
								String.format(
										Locale.ROOT,
										"Function expects arguments to be of type %s, but %s found",
										javaType.getName(),
										sqmExpression.getExpressableType().getJavaType()
								)
						);
					}
				}
		);
	}

	public static ArgumentsValidator composite(ArgumentsValidator... validators) {
		return composite( Arrays.asList( validators ) );
	}

	public static ArgumentsValidator composite(List<ArgumentsValidator> validators) {
		return arguments -> validators.forEach(
				individualValidator -> individualValidator.validate( arguments )
		);
	}
}
