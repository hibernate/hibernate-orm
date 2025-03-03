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
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * A special table group for a sub-queries.
 *
 * @author Christian Beikov
 */
public class QueryPartTableGroup extends AbstractTableGroup {

	private final QueryPartTableReference queryPartTableReference;
	private final Set<String> compatibleTableExpressions;

	public QueryPartTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			SelectStatement selectStatement,
			String sourceAlias,
			List<String> columnNames,
			boolean lateral,
			boolean canUseInnerJoins,
			SessionFactoryImplementor sessionFactory) {
		this(
				navigablePath,
				tableGroupProducer,
				selectStatement,
				sourceAlias,
				columnNames,
				Collections.emptySet(),
				lateral,
				canUseInnerJoins,
				sessionFactory
		);
	}

	public QueryPartTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			SelectStatement selectStatement,
			String sourceAlias,
			List<String> columnNames,
			Set<String> compatibleTableExpressions,
			boolean lateral,
			boolean canUseInnerJoins,
			SessionFactoryImplementor sessionFactory) {
		super(
				canUseInnerJoins,
				navigablePath,
				tableGroupProducer,
				sourceAlias,
				null,
				sessionFactory
		);
		this.compatibleTableExpressions = compatibleTableExpressions;
		this.queryPartTableReference = new QueryPartTableReference(
				selectStatement,
				sourceAlias,
				columnNames,
				lateral,
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
		if ( compatibleTableExpressions.contains( tableExpression ) ) {
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
		queryPartTableReference.applyAffectedTableNames( nameCollector );
	}

	@Override
	public QueryPartTableReference getPrimaryTableReference() {
		return queryPartTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

}
