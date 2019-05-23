/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * Defines the comparison operators.  We could also get away with
 * only 3 and use negation...
 */
public enum ComparisonOperator {
	EQUAL {
		public ComparisonOperator negated() {
			return NOT_EQUAL;
		}

		@Override
		public String sqlText() {
			return "=";
		}
	},

	NOT_EQUAL {
		public ComparisonOperator negated() {
			return EQUAL;
		}

		@Override
		public String sqlText() {
			return "!=";
		}
	},

	LESS_THAN {
		public ComparisonOperator negated() {
			return GREATER_THAN_OR_EQUAL;
		}

		@Override
		public String sqlText() {
			return "<";
		}
	},

	LESS_THAN_OR_EQUAL {
		public ComparisonOperator negated() {
			return GREATER_THAN;
		}

		@Override
		public String sqlText() {
			return "<=";
		}
	},

	GREATER_THAN {
		public ComparisonOperator negated() {
			return LESS_THAN_OR_EQUAL;
		}

		@Override
		public String sqlText() {
			return ">";
		}
	},

	GREATER_THAN_OR_EQUAL {
		public ComparisonOperator negated() {
			return LESS_THAN;
		}

		@Override
		public String sqlText() {
			return ">=";
		}
	};

	public abstract ComparisonOperator negated();
	public abstract String sqlText();
}
