/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.criteria.JpaCriteriaBase;

/**
 * Commonality between a top-level statement and a sub-query
 *
 * @author Steve Ebersole
 */
public interface SqmQuery<T> extends JpaCriteriaBase, SqmNode {
	@Override
	SqmQuery<T> copy(SqmCopyContext context);

	String generateAlias();
}
