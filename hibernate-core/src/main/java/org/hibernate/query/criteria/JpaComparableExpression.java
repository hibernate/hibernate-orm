/*
 * SPDX-License-Identifier: Apache-2.0
 *  Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.ComparableExpression;

/// API extension to the JPA {@link ComparableExpression} contract
///
/// @author Steve Ebersole
public interface JpaComparableExpression<C extends Comparable<? super C>> extends ComparableExpression<C> {
}
