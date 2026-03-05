/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.spi.NavigablePath;

/**
 * Table reference that matches both an auxiliary table expression
 * (history or audit table) and the current table expression.
 */
public class AuxiliaryTableReference extends NamedTableReference {
	private final String currentTableExpression;

	public AuxiliaryTableReference(
			String auxiliaryTableExpression,
			String currentTableExpression,
			String identificationVariable) {
		super( auxiliaryTableExpression, identificationVariable );
		this.currentTableExpression = currentTableExpression;
	}

	public AuxiliaryTableReference(
			String auxiliaryTableExpression,
			String currentTableExpression,
			String identificationVariable,
			boolean isOptional) {
		super( auxiliaryTableExpression, identificationVariable, isOptional );
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
