/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.spi.NavigablePath;

/**
 * Table reference that matches both the history table expression and the current table expression.
 */
public class HistoryTableReference extends NamedTableReference {
	private final String currentTableExpression;

	public HistoryTableReference(
			String historyTableExpression,
			String currentTableExpression,
			String identificationVariable) {
		super( historyTableExpression, identificationVariable );
		this.currentTableExpression = currentTableExpression;
	}

	public HistoryTableReference(
			String historyTableExpression,
			String currentTableExpression,
			String identificationVariable,
			boolean isOptional) {
		super( historyTableExpression, identificationVariable, isOptional );
		this.currentTableExpression = currentTableExpression;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		return currentTableExpression.equals( tableExpression )
				? this
				: super.getTableReference( navigablePath, tableExpression, resolve );
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression) {
		return currentTableExpression.equals( tableExpression )
				? this
				: super.resolveTableReference( navigablePath, tableExpression );
	}

	@Override
	public boolean containsAffectedTableName(String requestedName) {
		return super.containsAffectedTableName( requestedName )
			|| currentTableExpression.equals( requestedName );
	}
}
