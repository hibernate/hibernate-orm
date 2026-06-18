/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmSingularValuedJoin<L,R> extends SqmJoin<L, R> {
	SqmCorrelatedSingularValuedJoin<L,R> createCorrelation();
}
