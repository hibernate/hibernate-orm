/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
