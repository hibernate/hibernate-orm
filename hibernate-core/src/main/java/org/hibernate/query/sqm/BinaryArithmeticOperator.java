/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

/**
 * Enumeration of standard binary arithmetic operators
 *
 * @author Steve Ebersole
 */
public enum BinaryArithmeticOperator {
	ADD {
		@Override
		public String toLoggableText(String lhs, String rhs) {
			return standardToLoggableText( lhs, this, rhs );
		}

		@Override
		public char getOperatorSqlText() {
			return '+';
		}
	},

	SUBTRACT {
		@Override
		public String toLoggableText(String lhs, String rhs) {
			return standardToLoggableText( lhs, this, rhs );
		}

		@Override
		public char getOperatorSqlText() {
			return '-';
		}
	},

	MULTIPLY {
		@Override
		public String toLoggableText(String lhs, String rhs) {
			return standardToLoggableText( lhs, this, rhs );
		}

		@Override
		public char getOperatorSqlText() {
			return '*';
		}
	},

	DIVIDE {
		@Override
		public String toLoggableText(String lhs, String rhs) {
			return standardToLoggableText( lhs, this, rhs );
		}

		@Override
		public char getOperatorSqlText() {
			return '/';
		}
	},

	QUOT {
		@Override
		public String toLoggableText(String lhs, String rhs) {
			return standardToLoggableText( lhs, this, rhs );
		}

		@Override
		public char getOperatorSqlText() {
			return '/';
		}
	},

	MODULO {
		@Override
		public String toLoggableText(String lhs, String rhs) {
//				return lhs + " % " + rhs;
			return "mod(" + lhs + "," + rhs + ")";
		}

		@Override
		public char getOperatorSqlText() {
			return '%';
		}
	},

	/**
	 * "Portable" division, that is, true integer division when the
	 * operands are integers.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PORTABLE_INTEGER_DIVISION
	 * @see org.hibernate.query.spi.QueryEngineOptions#isPortableIntegerDivisionEnabled()
	 */
	DIVIDE_PORTABLE {
		@Override
		public String toLoggableText(String lhs, String rhs) {
			return standardToLoggableText( lhs, this, rhs );
		}

		@Override
		public char getOperatorSqlText() {
			return '/';
		}
	},

	;

	public abstract String toLoggableText(String lhs, String rhs);
	public abstract char getOperatorSqlText();

	public String getOperatorSqlTextString() {
		return Character.toString( getOperatorSqlText() );
	}

	private static String standardToLoggableText(String lhs, BinaryArithmeticOperator operator, String rhs) {
		return standardToLoggableText( lhs, operator.getOperatorSqlText(), rhs );
	}

	private static String standardToLoggableText(String lhs, char operator, String rhs) {
		return '(' + lhs + operator + rhs + ')';
	}

}
