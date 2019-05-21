/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

/**
 * A representation of SqmInsertSelectStatement at the
 * {@link org.hibernate.query.criteria} level, even though JPA does
 * not define support for insert-select criteria.
 *
 * @see org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement
 *
 * @author Steve Ebersole
 */
public interface JpaCriteriaInsertSelect<T> extends JpaManipulationCriteria<T> {
}
