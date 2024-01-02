/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.Incubating;

/**
 * A representation of SqmInsertValuesStatement at the
 * {@link org.hibernate.query.criteria} level, even though JPA does
 * not define support for insert-values criteria.
 *
 * @see org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement
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
 * @author Gavin King
 */
@Incubating
public interface JpaCriteriaInsertValues<T> extends JpaCriteriaInsert<T> {

	JpaCriteriaInsertValues<T> values(JpaValues... values);

	JpaCriteriaInsertValues<T> values(List<? extends JpaValues> values);

	@Override
	JpaCriteriaInsertValues<T> onConflict(JpaConflictClause<T> conflictClause);

}
