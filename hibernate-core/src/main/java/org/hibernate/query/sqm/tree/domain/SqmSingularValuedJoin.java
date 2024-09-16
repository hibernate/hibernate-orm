/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.tree.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmSingularValuedJoin<L,R> extends SqmJoin<L, R> {
	SqmCorrelatedSingularValuedJoin<L,R> createCorrelation();
}
