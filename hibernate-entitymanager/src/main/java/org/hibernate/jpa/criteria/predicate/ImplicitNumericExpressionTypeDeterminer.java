/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.predicate;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Steve Ebersole
 */
public final class ImplicitNumericExpressionTypeDeterminer {
	private ImplicitNumericExpressionTypeDeterminer() {
	}

	/**
	 * Determine the appropriate runtime result type for a numeric expression according to
	 * section "6.5.7.1 Result Types of Expressions" of the JPA spec.
	 * <p/>
	 * Note that it is expected that the caveats about quotient handling have already been handled.
	 *
	 * @param types The argument/expression types
	 *
	 * @return The appropriate numeric result type.
	 */
	public static Class<? extends Number> determineResultType(Class<? extends Number>... types) {
		Class<? extends Number> result = Number.class;

		for ( Class<? extends Number> type : types ) {
			if ( Double.class.equals( type ) ) {
				result = Double.class;
			}
			else if ( Float.class.equals( type ) ) {
				result = Float.class;
			}
			else if ( BigDecimal.class.equals( type ) ) {
				result = BigDecimal.class;
			}
			else if ( BigInteger.class.equals( type ) ) {
				result = BigInteger.class;
			}
			else if ( Long.class.equals( type ) ) {
				result = Long.class;
			}
			else if ( isIntegralType( type ) ) {
				result = Integer.class;
			}
		}

		return result;
	}

	private static boolean isIntegralType(Class<? extends Number> type) {
		return Integer.class.equals( type ) ||
				Short.class.equals( type );

	}
}
