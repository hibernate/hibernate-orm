/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

/**
 * Unified contract for things that can contain a SqmWhereClause.
 *
 * @author Steve Ebersole
 */
public interface SqmWhereClauseContainer {
	SqmWhereClause getWhereClause();

	void applyPredicate(SqmPredicate accept);
}
