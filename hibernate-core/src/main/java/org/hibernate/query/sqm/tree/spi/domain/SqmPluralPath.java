package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.criteria.JpaPluralExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmPluralPath<C,E> extends SqmPath<C>, JpaPluralExpression<C,E> {
}
