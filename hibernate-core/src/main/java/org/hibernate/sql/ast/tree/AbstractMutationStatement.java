/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import java.util.List;

import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.model.MutationTarget;

/**
 * @author Christian Beikov
 */
public abstract class AbstractMutationStatement extends AbstractStatement implements MutationStatement {

	private final NamedTableReference targetTable;
	private final List<ColumnReference> returningColumns;
	private final MutationTarget<?> mutationTarget;

	public AbstractMutationStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			List<ColumnReference> returningColumns,
			MutationTarget<?> mutationTarget) {
		super( cteContainer );
		this.targetTable = targetTable;
		this.returningColumns = returningColumns;
		this.mutationTarget = mutationTarget;
	}

	@Override
	public NamedTableReference getTargetTable() {
		return targetTable;
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return returningColumns;
	}

	@Override
	public MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}
}
