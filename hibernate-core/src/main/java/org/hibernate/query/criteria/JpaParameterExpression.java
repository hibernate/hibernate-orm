/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.ParameterExpression;

/**
 * @author Steve Ebersole
 */
public interface JpaParameterExpression<T> extends ParameterExpression<T>, JpaCriteriaNode {
}
