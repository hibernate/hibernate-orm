/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query;

import java.util.List;

import org.hibernate.sql.ast.spi.query.cte.CteContainer;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.spi.mutation.MutationTarget;

/**
 * @author Christian Beikov
 */
public abstract class AbstractMutationStatement extends AbstractStatement implements MutationStatement {

	private final NamedTableReference targetTable;
	private final List<ColumnReference> returningColumns;
	private final MutationTarget mutationTarget;

	public AbstractMutationStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			MutationTarget mutationTarget,
			List<ColumnReference> returningColumns) {
		super( cteContainer );
		this.targetTable = targetTable;
		this.mutationTarget = mutationTarget;
		this.returningColumns = returningColumns;
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
	public MutationTarget getMutationTarget() {
		return mutationTarget;
	}
}
