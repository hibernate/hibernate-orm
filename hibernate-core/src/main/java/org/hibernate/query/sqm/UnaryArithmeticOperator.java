/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
