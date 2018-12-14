/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public class Junction extends AbstractPredicate {
	private final BooleanOperator operator;

	private List<ExpressionImplementor<Boolean>> expressions;

	protected Junction(BooleanOperator operator, CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );

		this.operator = operator;
	}

	protected Junction(
			BooleanOperator operator,
			List<ExpressionImplementor<Boolean>> expressions,
			CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );

		this.operator = operator;
		this.expressions = expressions;
	}

	@Override
	public BooleanOperator getOperator() {
		return operator;
	}

	@Override
	@SuppressWarnings({"unchecked", "RedundantCast"})
	public List<Expression<Boolean>> getExpressions() {
		return expressions == null ? Collections.emptyList() : (List) expressions;
	}

	public Junction visitExpressions(Consumer<ExpressionImplementor> consumer) {
		if ( expressions != null ) {
			expressions.forEach( consumer );
		}
		return this;
	}

	public Junction addExpression(ExpressionImplementor<Boolean> expression) {
		if ( expressions == null ) {
			expressions = new ArrayList<>();
		}
		expressions.add( expression );
		return this;
	}

	@Override
	public <T> T accept(CriteriaVisitor visitor) {
		return visitor.visitJunctionPredicate( this );
	}
}
