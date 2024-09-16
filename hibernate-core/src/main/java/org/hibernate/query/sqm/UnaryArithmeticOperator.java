/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * @author Steve Ebersole
 */
public enum UnaryArithmeticOperator {
	UNARY_PLUS( '+' ),
	UNARY_MINUS( '-' );

	private final char operatorChar;

	UnaryArithmeticOperator(char operatorChar) {
		this.operatorChar = operatorChar;
	}

	public char getOperatorChar() {
		return operatorChar;
	}
}
