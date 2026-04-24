/*
 * SPDX-License-Identifier: Apache-2.0
 *  Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.NumericExpression;

/// API extension to the JPA {@link NumericExpression} contract
///
/// @author Steve Ebersole
public interface JpaNumericExpression<N extends Number & Comparable<N>>
		extends NumericExpression<N>, JpaComparableExpression<N> {
}
