/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

/**
 * @author Steve Ebersole
 */
public interface SqmCorrelatedSingularValuedJoin<L,R> extends SqmSingularValuedJoin<L, R>, SqmCorrelatedJoin<L, R> {
}
