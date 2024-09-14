/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

/**
 * Defines the comparison operators.  We could also get away with
 * only 3 and use negation...
 *
 * @author Steve Ebersole
 */
public enum ComparisonOperator {
	EQUAL {
		@Override
		public ComparisonOperator negated() {
			return NOT_EQUAL;
		}

		@Override
		public ComparisonOperator invert() {
			return EQUAL;
		}

		@Override
		public ComparisonOperator broader() {
			return EQUAL;
		}

		@Override
		public ComparisonOperator sharper() {
			return EQUAL;
		}

		@Override
		public String sqlText() {
			return "=";
		}
	},

	NOT_EQUAL {
		@Override
		public ComparisonOperator negated() {
			return EQUAL;
		}

		@Override
		public ComparisonOperator invert() {
			return NOT_EQUAL;
		}

		@Override
		public ComparisonOperator broader() {
			return NOT_EQUAL;
		}

		@Override
		public ComparisonOperator sharper() {
			return NOT_EQUAL;
		}

		@Override
		public String sqlText() {
			return "<>";
		}
	},
	NOT_DISTINCT_FROM {
		@Override
		public ComparisonOperator negated() {
			return DISTINCT_FROM;
		}

		@Override
		public ComparisonOperator invert() {
			return NOT_DISTINCT_FROM;
		}

		@Override
		public ComparisonOperator broader() {
			return NOT_DISTINCT_FROM;
		}

		@Override
		public ComparisonOperator sharper() {
			return NOT_DISTINCT_FROM;
		}

		@Override
		public String sqlText() {
			return " is not distinct from ";
		}
	},

	DISTINCT_FROM {
		@Override
		public ComparisonOperator negated() {
			return NOT_DISTINCT_FROM;
		}

		@Override
		public ComparisonOperator invert() {
			return DISTINCT_FROM;
		}

		@Override
		public ComparisonOperator broader() {
			return DISTINCT_FROM;
		}

		@Override
		public ComparisonOperator sharper() {
			return DISTINCT_FROM;
		}

		@Override
		public String sqlText() {
			return " is distinct from ";
		}
	},

	LESS_THAN {
		@Override
		public ComparisonOperator negated() {
			return GREATER_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator invert() {
			return GREATER_THAN;
		}

		@Override
		public ComparisonOperator broader() {
			return LESS_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator sharper() {
			return LESS_THAN;
		}

		@Override
		public String sqlText() {
			return "<";
		}
	},

	LESS_THAN_OR_EQUAL {
		@Override
		public ComparisonOperator negated() {
			return GREATER_THAN;
		}

		@Override
		public ComparisonOperator invert() {
			return GREATER_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator broader() {
			return LESS_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator sharper() {
			return LESS_THAN;
		}

		@Override
		public String sqlText() {
			return "<=";
		}
	},

	GREATER_THAN {
		@Override
		public ComparisonOperator negated() {
			return LESS_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator invert() {
			return LESS_THAN;
		}

		@Override
		public ComparisonOperator broader() {
			return GREATER_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator sharper() {
			return GREATER_THAN;
		}

		@Override
		public String sqlText() {
			return ">";
		}
	},

	GREATER_THAN_OR_EQUAL {
		@Override
		public ComparisonOperator negated() {
			return LESS_THAN;
		}

		@Override
		public ComparisonOperator invert() {
			return LESS_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator broader() {
			return GREATER_THAN_OR_EQUAL;
		}

		@Override
		public ComparisonOperator sharper() {
			return GREATER_THAN;
		}

		@Override
		public String sqlText() {
			return ">=";
		}
	};

	public abstract ComparisonOperator negated();
	public abstract ComparisonOperator invert();
	public abstract ComparisonOperator broader();
	public abstract ComparisonOperator sharper();
	public abstract String sqlText();
}
