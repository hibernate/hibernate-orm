/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * A SQM window function.
 *
 * @param <T> The Java type of the expression
 *
 * @author Christian Beikov
 */
public interface SqmWindowFunction<T> extends JpaFunction<T>, SqmExpression<T> {

	SqmPredicate getFilter();

	Boolean getRespectNulls();

	Boolean getFromFirst();
}
