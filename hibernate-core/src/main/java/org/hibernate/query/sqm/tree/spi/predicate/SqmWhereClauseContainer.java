package org.hibernate.query.sqm.tree.spi.predicate;

import jakarta.annotation.Nullable;

/**
 * Unified contract for things that can contain a SqmWhereClause.
 *
 * @author Steve Ebersole
 */
public interface SqmWhereClauseContainer {
	@Nullable SqmWhereClause getWhereClause();

	void applyPredicate(SqmPredicate accept);
}
