/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.query.Query;

import jakarta.persistence.criteria.CommonAbstractCriteria;

/**
 * Common contract for the forms of criteria that are "queryable" - can be
 * converted into a {@link Query}.
 *
 * Hibernate extension to the JPA {@link CommonAbstractCriteria} contract.
 *
 * @see JpaCriteriaQuery
 * @see JpaCriteriaDelete
 * @see JpaCriteriaUpdate
 *
 * @author Steve Ebersole
 */
public interface JpaQueryableCriteria<T> extends JpaCriteriaBase, JpaCriteriaNode, JpaCteContainer {
}
