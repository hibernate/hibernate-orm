/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.tree.select.SqmOrderByClause;

/**
 * A SQM ordered set-aggregate function.
 *
 * @param <T> The Java type of the expression
 *
 * @author Christian Beikov
 */
public interface SqmOrderedSetAggregateFunction<T> extends SqmAggregateFunction<T> {

	SqmOrderByClause getWithinGroup();
}
