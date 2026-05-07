/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import java.util.List;

import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.MutationTarget;

public abstract class AbstractUpdateOrDeleteStatement extends AbstractMutationStatement {
	private final FromClause fromClause;
	private final Predicate restriction;

	public AbstractUpdateOrDeleteStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			MutationTarget<?> mutationTarget,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, mutationTarget, returningColumns );
		this.fromClause = fromClause;
		this.restriction = restriction;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public Predicate getRestriction() {
		return restriction;
	}
}
