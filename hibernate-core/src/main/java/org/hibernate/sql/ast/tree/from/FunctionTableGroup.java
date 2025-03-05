/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;

/**
 * A special table group for a table valued functions.
 *
 * @author Christian Beikov
 */
public class FunctionTableGroup extends AbstractTableGroup {

	private final FunctionTableReference functionTableReference;

	public FunctionTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			FunctionExpression functionExpression,
			String sourceAlias,
			List<String> columnNames,
			Set<String> compatibleTableExpressions,
			boolean lateral,
			boolean canUseInnerJoins,
			boolean rendersIdentifierVariable,
			SessionFactoryImplementor sessionFactory) {
		super(
				canUseInnerJoins,
				navigablePath,
				tableGroupProducer,
				sourceAlias,
				null,
				sessionFactory
		);
		this.functionTableReference = new FunctionTableReference(
				functionExpression,
				sourceAlias,
				columnNames,
				lateral,
				rendersIdentifierVariable,
				compatibleTableExpressions,
				sessionFactory
		);
	}

	@Override
	public boolean isLateral() {
		return getPrimaryTableReference().isLateral();
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		if ( getPrimaryTableReference().containsAffectedTableName( tableExpression ) ) {
			return getPrimaryTableReference();
		}
		for ( TableGroupJoin tableGroupJoin : getNestedTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		return null;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		getPrimaryTableReference().applyAffectedTableNames( nameCollector );
	}

	@Override
	public FunctionTableReference getPrimaryTableReference() {
		return functionTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

}
