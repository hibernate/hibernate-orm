package org.hibernate.query.criteria;

import jakarta.persistence.criteria.BooleanExpression;

/// API extension to the JPA {@link BooleanExpression} contract
///
/// @author Steve Ebersole
public interface JpaBooleanExpression extends BooleanExpression, JpaComparableExpression<Boolean> {
}
