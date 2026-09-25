package org.hibernate.query.criteria;

import jakarta.persistence.criteria.ParameterExpression;

/**
 * @author Steve Ebersole
 */
public interface JpaParameterExpression<T> extends ParameterExpression<T>, JpaCriteriaNode {
}
