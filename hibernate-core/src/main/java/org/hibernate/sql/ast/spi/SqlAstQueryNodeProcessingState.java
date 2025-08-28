/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.Map;

import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * SqlAstProcessingState specialization for query parts
 */
public interface SqlAstQueryNodeProcessingState extends SqlAstProcessingState {

	/**
	 * Returns the in-flight from clause for the query node.
	 */
	FromClause getFromClause();

	/**
	 * Apply the predicate to be used for the final statement.
	 */
	void applyPredicate(Predicate predicate);

	/**
	 * Registers that the given SqmFrom is treated.
	 */
	void registerTreatedFrom(SqmFrom<?, ?> sqmFrom);

	/**
	 * Registers that the given SqmFrom was used in an expression and whether to downgrade {@link org.hibernate.persister.entity.EntityNameUse#TREAT} of it.
	 */
	void registerFromUsage(SqmFrom<?, ?> sqmFrom, boolean downgradeTreatUses);

	/**
	 * Returns the treated SqmFroms and whether their {@link org.hibernate.persister.entity.EntityNameUse#TREAT}
	 * should be downgraded to {@link org.hibernate.persister.entity.EntityNameUse#EXPRESSION}.
	 */
	Map<SqmFrom<?, ?>, Boolean> getFromRegistrations();
}
