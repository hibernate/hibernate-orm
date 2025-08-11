/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * The JPA-standard criteria query API defines all the operations needed express
 * any query written in standard JPQL. This package extends the JPA-defined API,
 * allowing any query written in HQL to be expressed via the criteria API.
 * <p>
 * The gateway to this functionality is
 * {@link org.hibernate.query.criteria.HibernateCriteriaBuilder}, which extends
 * {@link jakarta.persistence.criteria.CriteriaBuilder}.
 * <p>
 * Types defined in this package extend the equivalent types in
 * {@link jakarta.persistence.criteria} with additional operations. For example,
 * {@link org.hibernate.query.criteria.JpaCriteriaQuery} adds the methods:
 * <ul>
 * <li>{@link org.hibernate.query.criteria.JpaCriteriaQuery#from(Subquery)},
 *     which allows the use of a subquery in the {@code from} clause of the
 *     query, and
 * <li>{@link org.hibernate.query.criteria.JpaCriteriaQuery#with(AbstractQuery)},
 *     which allows the creation of {@link org.hibernate.query.criteria.JpaCteCriteria
 *     common table expressions}.
 * </ul>
 * <p>
 * The very useful operation {@link
 * org.hibernate.query.criteria.HibernateCriteriaBuilder#createQuery(java.lang.String, java.lang.Class)}
 * transforms a given HQL query string to an equivalent criteria query.
 * <p>
 * The class {@link org.hibernate.query.criteria.CriteriaDefinition} is a helpful
 * utility that makes it easier to construct criteria queries.
 *
 * @see org.hibernate.query.criteria.HibernateCriteriaBuilder
 * @see org.hibernate.query.criteria.JpaCriteriaQuery
 * @see org.hibernate.query.criteria.JpaCriteriaUpdate
 * @see org.hibernate.query.criteria.JpaCriteriaDelete
 * @see org.hibernate.query.criteria.JpaCriteriaInsertValues
 * @see org.hibernate.query.criteria.JpaCriteriaInsertSelect
 * @see org.hibernate.query.criteria.JpaCteCriteria
 * @see org.hibernate.query.criteria.JpaSubQuery
 * @see org.hibernate.query.criteria.JpaExpression
 *
 * @see org.hibernate.query.criteria.HibernateCriteriaBuilder#createQuery(java.lang.String, java.lang.Class)
 * @see org.hibernate.query.criteria.CriteriaDefinition
 */
@Incubating
package org.hibernate.query.criteria;

import org.hibernate.Incubating;
