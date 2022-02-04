/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * A special table group for a sub-queries.
 *
 * @author Christian Beikov
 */
public class QueryPartTableGroup extends AbstractTableGroup {

	private final QueryPartTableReference queryPartTableReference;
	private final String compatibleTableExpression;

	public QueryPartTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			QueryPart queryPart,
			String sourceAlias,
			List<String> columnNames,
			boolean lateral,
			boolean canUseInnerJoins,
			SessionFactoryImplementor sessionFactory) {
		this(
				navigablePath,
				tableGroupProducer,
				queryPart,
				sourceAlias,
				columnNames,
				null,
				lateral,
				canUseInnerJoins,
				sessionFactory
		);
	}

	public QueryPartTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			QueryPart queryPart,
			String sourceAlias,
			List<String> columnNames,
			String compatibleTableExpression, boolean lateral,
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
		this.compatibleTableExpression = compatibleTableExpression;
		this.queryPartTableReference = new QueryPartTableReference(
				queryPart,
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
	protected TableReference getTableReferenceInternal(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		if ( Objects.equals( tableExpression, compatibleTableExpression ) ) {
			return getPrimaryTableReference();
		}
		for ( TableGroupJoin tableGroupJoin : getNestedTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
			if ( groupTableReference != null ) {
				return groupTableReference;
			}
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
					.getPrimaryTableReference()
					.getTableReference( navigablePath, tableExpression, allowFkOptimization, resolve );
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
