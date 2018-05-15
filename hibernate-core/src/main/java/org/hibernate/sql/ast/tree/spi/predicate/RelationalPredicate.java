/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.predicate;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class RelationalPredicate implements Predicate {
	public enum Operator {
		EQUAL {
			@Override
			public Operator negate() {
				return NOT_EQUAL;
			}

			@Override
			public String sqlText() {
				return "=";
			}
		},
		NOT_EQUAL {
			@Override
			public Operator negate() {
				return EQUAL;
			}

			@Override
			public String sqlText() {
				return "<>";
			}
		},
		GT {
			@Override
			public Operator negate() {
				return LE;
			}

			@Override
			public String sqlText() {
				return ">";
			}
		},
		GE {
			@Override
			public Operator negate() {
				return LT;
			}

			@Override
			public String sqlText() {
				return ">=";
			}
		},
		LT {
			@Override
			public Operator negate() {
				return GE;
			}

			@Override
			public String sqlText() {
				return "<";
			}
		},

		LE {
			@Override
			public Operator negate() {
				return GT;
			}

			@Override
			public String sqlText() {
				return "<=";
			}
		};

		public abstract Operator negate();
		public abstract String sqlText();
	}

	private final Expression leftHandExpression;
	private final Expression rightHandExpression;
	private Operator operator;

	public RelationalPredicate(
			Operator operator,
			Expression leftHandExpression,
			Expression rightHandExpression) {
		this.leftHandExpression = leftHandExpression;
		this.rightHandExpression = rightHandExpression;
		this.operator = operator;
	}

	public Expression getLeftHandExpression() {
		return leftHandExpression;
	}

	public Expression getRightHandExpression() {
		return rightHandExpression;
	}

	public Operator getOperator() {
		return operator;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitRelationalPredicate( this );
	}
}
