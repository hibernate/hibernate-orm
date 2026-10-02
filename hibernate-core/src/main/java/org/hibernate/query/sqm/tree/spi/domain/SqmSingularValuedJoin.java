package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nonnull;

import org.hibernate.query.sqm.tree.spi.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmSingularValuedJoin<L,R> extends SqmJoin<L, R> {
	@Nonnull
	SqmCorrelatedSingularValuedJoin<L,R> createCorrelation();
}
