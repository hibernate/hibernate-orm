/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.Locale;

import org.hibernate.query.sqm.QueryException;

/**
 * @author Steve Ebersole
 */
public final class StandardArgumentsValidators {
	private StandardArgumentsValidators() {
	}

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

	public static ArgumentsValidator count(int number) {
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
					if ( !javaType.isInstance( sqmExpression.getExpressionType().getJavaType() ) ) {
						throw new QueryException(
								String.format(
										Locale.ROOT,
										"Function expects arguments to be of type %s, but %s found",
										javaType.getName(),
										sqmExpression.getExpressionType().getJavaType()
								)
						);
					}
				}
		);
	}
}
