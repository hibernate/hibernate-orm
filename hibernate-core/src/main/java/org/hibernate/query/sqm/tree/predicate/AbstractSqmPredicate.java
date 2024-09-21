/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;

import jakarta.persistence.criteria.Expression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPredicate extends AbstractSqmExpression<Boolean> implements SqmPredicate {

	public AbstractSqmPredicate(SqmExpressible<Boolean> type, NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
	}

	@Override
	public BooleanOperator getOperator() {
		// most predicates are conjunctive
		return BooleanOperator.AND;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		/// most predicates do not have sub-predicates
		return new ArrayList<>(0);
	}

}
