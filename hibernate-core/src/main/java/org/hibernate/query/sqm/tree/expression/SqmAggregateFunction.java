/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * A SQM aggregate function.
 *
 * @param <T> The Java type of the expression
 *
 * @author Christian Beikov
 */
public interface SqmAggregateFunction<T> extends JpaFunction<T>, SqmExpression<T> {

	SqmPredicate getFilter();
}
