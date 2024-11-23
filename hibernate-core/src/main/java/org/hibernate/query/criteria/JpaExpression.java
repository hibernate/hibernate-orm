/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * API extension to the JPA {@link Expression} contract
 *
 * @author Steve Ebersole
 */
public interface JpaExpression<T> extends JpaSelection<T>, Expression<T> {

	JpaExpression<Long> asLong();

	JpaExpression<Integer> asInteger();

	JpaExpression<Float> asFloat();

	JpaExpression<Double> asDouble();

	JpaExpression<BigDecimal> asBigDecimal();

	JpaExpression<BigInteger> asBigInteger();

	JpaExpression<String> asString();

	@Override
	<X> JpaExpression<X> as(Class<X> type);

	@Override
	JpaPredicate isNull();

	@Override
	JpaPredicate isNotNull();

	@Override
	JpaPredicate in(Object... values);

	@Override
	JpaPredicate in(Expression<?>... values);

	@Override
	JpaPredicate in(Collection<?> values);

	@Override
	JpaPredicate in(Expression<Collection<?>> values);

	@Override
	Predicate equalTo(Expression<?> value);

	@Override
	Predicate equalTo(Object value);

	@Override
	<X> JpaExpression<X> cast(Class<X> type);

	@Override
	Predicate notEqualTo(Expression<?> value);

	@Override
	Predicate notEqualTo(Object value);
}
