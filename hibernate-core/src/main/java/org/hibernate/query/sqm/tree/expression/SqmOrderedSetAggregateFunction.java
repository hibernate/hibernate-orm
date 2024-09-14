/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
