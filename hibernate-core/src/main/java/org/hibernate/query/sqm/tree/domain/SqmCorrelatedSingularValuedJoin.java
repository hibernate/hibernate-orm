/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

/**
 * @author Steve Ebersole
 */
public interface SqmCorrelatedSingularValuedJoin<L,R> extends SqmSingularValuedJoin<L, R>, SqmCorrelatedJoin<L, R> {
}
