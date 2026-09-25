package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmNumericPath<N extends Number & Comparable<N>> extends SqmPath<N>, SqmNumericExpression<N> {
}
