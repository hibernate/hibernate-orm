/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Unified contract for things that can contain a SqmWhereClause.
 *
 * @author Steve Ebersole
 */
public interface SqmWhereClauseContainer {
	@Nullable SqmWhereClause getWhereClause();

	void applyPredicate(SqmPredicate accept);
}
