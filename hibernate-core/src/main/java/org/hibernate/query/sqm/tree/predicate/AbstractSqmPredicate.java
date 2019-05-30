/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA Expression

	@Override
	public SqmPredicate isNull() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPredicate isNotNull() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPredicate in(Object... values) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPredicate in(Expression<?>... values) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPredicate in(Collection<?> values) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<Long> asLong() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<Float> asFloat() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<Double> asDouble() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmExpression<String> asString() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return Collections.emptyList();
	}

	@Override
	public JpaSelection<Boolean> alias(String name) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public String getAlias() {
		throw new NotYetImplementedFor6Exception();
	}
}
