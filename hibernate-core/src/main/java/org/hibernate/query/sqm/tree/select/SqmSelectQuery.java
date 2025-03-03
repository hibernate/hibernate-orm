/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.criteria.JpaSelectCriteria;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmNode;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;

/**
 * Common contract between a {@linkplain SqmSelectStatement root} and a
 * {@link SqmSubQuery sub-query}
 *
 * @author Steve Ebersole
 */
public interface SqmSelectQuery<T> extends SqmQuery<T>, JpaSelectCriteria<T>, SqmNode, SqmCteContainer {
	@Override
	SqmQuerySpec<T> getQuerySpec();

	SqmQueryPart<T> getQueryPart();

	@Override
	SqmSelectQuery<T> distinct(boolean distinct);

	@Override
	SqmSelectQuery<T> copy(SqmCopyContext context);
}
