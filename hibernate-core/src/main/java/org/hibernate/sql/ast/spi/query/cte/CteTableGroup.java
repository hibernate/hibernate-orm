/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.cte;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.query.from.AbstractTableGroup;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;

/**
 * Wraps a {@link NamedTableReference} representing the CTE and adapts it to
 * {@link TableGroup} for use in SQL AST
 *
 * @author Steve Ebersole
 */
public class CteTableGroup extends AbstractTableGroup {
	private final NamedTableReference cteTableReference;
	private final TableGroupProducer tableGroupProducer;

	public CteTableGroup(NamedTableReference cteTableReference) {
		this(
				false,
				new NavigablePath( cteTableReference.getTableExpression() ),
				null,
				null,
				cteTableReference
		);
	}

	public CteTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			SqlAliasBase sqlAliasBase,
			TableGroupProducer tableGroupProducer,
			NamedTableReference cteTableReference) {
		super(
				canUseInnerJoins,
				navigablePath,
				tableGroupProducer,
				cteTableReference.getIdentificationVariable(),
				sqlAliasBase,
				null
		);
		this.cteTableReference = cteTableReference;
		this.tableGroupProducer = tableGroupProducer;
	}

	@Override
	public String getGroupAlias() {
		return cteTableReference.getIdentificationVariable();
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		if ( tableGroupProducer != null && tableGroupProducer.containsTableReference( tableExpression ) ) {
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
		nameCollector.accept( cteTableReference.getTableExpression() );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return cteTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}
}
