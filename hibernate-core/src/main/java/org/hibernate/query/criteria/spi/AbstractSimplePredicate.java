/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;

/**
 * Base support for non-junction (AND/OR) predicates
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSimplePredicate extends AbstractPredicate {
	private static final List<Expression<Boolean>> NO_EXPRESSIONS = Collections.emptyList();

	private final boolean negated;

	public AbstractSimplePredicate(CriteriaNodeBuilder criteriaBuilder) {
		this( false, criteriaBuilder );
	}

	public AbstractSimplePredicate(boolean negated, CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		this.negated = negated;
	}

	@Override
	public boolean isNegated() {
		return negated;
	}

	@Override
	public BooleanOperator getOperator() {
		return BooleanOperator.AND;
	}

	@Override
	public final List<Expression<Boolean>> getExpressions() {
		return NO_EXPRESSIONS;
	}
}
