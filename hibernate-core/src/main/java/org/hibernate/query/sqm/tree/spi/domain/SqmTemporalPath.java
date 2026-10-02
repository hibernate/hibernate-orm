package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.expression.SqmTemporalExpression;

import java.time.temporal.Temporal;

/**
 * @author Steve Ebersole
 */
public interface SqmTemporalPath<T extends Temporal & Comparable<? super T>>
		extends SqmPath<T>, SqmTemporalExpression<T> {
}
