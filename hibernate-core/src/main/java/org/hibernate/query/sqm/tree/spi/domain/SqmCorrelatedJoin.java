package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmCorrelatedJoin<L,R> extends SqmCorrelation<L, R>, SqmJoin<L, R> {
}
