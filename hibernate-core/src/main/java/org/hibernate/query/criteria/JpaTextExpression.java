/*
 * SPDX-License-Identifier: Apache-2.0
 *  Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.TextExpression;

/// API extension to the JPA {@link TextExpression} contract
///
/// @author Steve Ebersole
public interface JpaTextExpression extends TextExpression, JpaComparableExpression<String> {
}
