/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Collections;
import java.util.List;
import jakarta.persistence.criteria.Expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPredicate extends AbstractSqmExpression<Boolean> implements SqmPredicate {
	@SuppressWarnings("WeakerAccess")
	public AbstractSqmPredicate(NodeBuilder criteriaBuilder) {
		//noinspection unchecked
		super( StandardBasicTypes.BOOLEAN, criteriaBuilder );
	}

	@Override
	public BooleanOperator getOperator() {
		// most predicates are conjunctive
		return BooleanOperator.AND;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		/// most predicates do not have sub-predicates
		return Collections.emptyList();
	}

}
