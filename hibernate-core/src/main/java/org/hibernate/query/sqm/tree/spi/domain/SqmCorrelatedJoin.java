/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmCorrelatedJoin<L,R> extends SqmCorrelation<L, R>, SqmJoin<L, R> {
}
