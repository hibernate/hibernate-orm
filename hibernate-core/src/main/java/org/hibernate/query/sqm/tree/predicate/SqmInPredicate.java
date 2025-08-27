/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.criteria.JpaInPredicate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmInPredicate<T> extends SqmNegatablePredicate, JpaInPredicate<T> {
	SqmExpression<T> getTestExpression();
}
