/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * A representation of SqmInsertSelectStatement at the
 * {@link org.hibernate.query.criteria} level, even though JPA does
 * not define support for insert-select criteria.
 *
 * @see org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement
 *
 * @apiNote Incubating mainly for 2 purposes:<ul>
 *     <li>
 *         to decide how to handle the typing for the "selection part".  Should it
 *         be {@code <T>} or {@code <X>}.  For the time being we expose it as
 *         {@code <T>} because that is what was done (without intention) originally,
 *         and it is the easiest form to validate
 *     </li>
 *     <li>
 *         Would be better to expose non-SQM contracts here
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JpaCriteriaInsertSelect<T> extends JpaCriteriaInsert<T> {

	JpaCriteriaInsertSelect<T> select(CriteriaQuery<Tuple> criteriaQuery);

	@Override
	JpaCriteriaInsertSelect<T> onConflict(@Nullable JpaConflictClause<T> conflictClause);
}
