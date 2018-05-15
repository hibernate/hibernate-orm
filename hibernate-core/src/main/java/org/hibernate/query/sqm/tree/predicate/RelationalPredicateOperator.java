/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

/**
 * @author Steve Ebersole
 */
public enum RelationalPredicateOperator {
	EQUAL {
		@Override
		public RelationalPredicateOperator negate() {
			return NOT_EQUAL;
		}
	},
	NOT_EQUAL {
		@Override
		public RelationalPredicateOperator negate() {
			return EQUAL;
		}
	},
	GREATER_THAN {
		@Override
		public RelationalPredicateOperator negate() {
			return LESS_THAN_OR_EQUAL;
		}
	},
	GREATER_THAN_OR_EQUAL {
		@Override
		public RelationalPredicateOperator negate() {
			return LESS_THAN;
		}
	},
	LESS_THAN {
		@Override
		public RelationalPredicateOperator negate() {
			return GREATER_THAN_OR_EQUAL;
		}
	},
	LESS_THAN_OR_EQUAL {
		@Override
		public RelationalPredicateOperator negate() {
			return GREATER_THAN;
		}
	};

	public abstract RelationalPredicateOperator negate();
}
