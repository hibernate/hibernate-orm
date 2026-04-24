/*
 * SPDX-License-Identifier: Apache-2.0
 *  Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.TemporalExpression;

import java.time.temporal.Temporal;

/// API extension to the JPA {@link TemporalExpression} contract
///
/// @author Steve Ebersole
public interface JpaTemporalExpression<T extends Temporal & Comparable<? super T>>
		extends TemporalExpression<T>, JpaComparableExpression<T> {
}
