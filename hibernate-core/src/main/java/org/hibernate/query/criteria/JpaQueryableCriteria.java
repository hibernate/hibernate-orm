/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CommonAbstractCriteria;

/**
 * Common contract for the forms of criteria that are "queryable" - can be
 * converted into a {@link org.hibernate.query.Query}.
 *
 * Hibernate extension to the JPA {@link CommonAbstractCriteria} contract.
 *
 * @see JpaCriteriaQuery
 * @see JpaCriteriaDelete
 * @see JpaCriteriaUpdate
 *
 * @author Steve Ebersole
 */
public interface JpaQueryableCriteria<T> extends JpaCriteriaBase, JpaCriteriaNode {
}
