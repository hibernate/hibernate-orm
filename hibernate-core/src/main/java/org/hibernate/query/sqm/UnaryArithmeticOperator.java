/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * Enumerates the unary prefix operators.
 *
 * @apiNote This is an SPI type allowing collaboration
 * between {@code org.hibernate.dialect} and
 * {@code org.hibernate.sqm}. It should never occur in
 * APIs visible to the application program.
 *
 * @author Steve Ebersole
 */
public enum UnaryArithmeticOperator {
	UNARY_PLUS,
	UNARY_MINUS;

	public char getOperatorChar() {
		return switch ( this ) {
			case UNARY_PLUS -> '+';
			case UNARY_MINUS -> '-';
		};
	}
}
