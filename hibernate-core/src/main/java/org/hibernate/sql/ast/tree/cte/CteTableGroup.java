/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * Wraps a {@link NamedTableReference} representing the CTE and adapts it to
 * {@link TableGroup} for use in SQL AST
 *
 * @author Steve Ebersole
 */
public class CteTableGroup extends AbstractTableGroup {
	private final NamedTableReference cteTableReference;
	private final Set<String> compatibleTableExpressions;

	public CteTableGroup(NamedTableReference cteTableReference) {
		this(
				false,
				new NavigablePath( cteTableReference.getTableExpression() ),
				null,
				null,
				cteTableReference,
				Collections.emptySet()
		);
	}

	public CteTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			SqlAliasBase sqlAliasBase,
			ModelPartContainer modelPartContainer,
			NamedTableReference cteTableReference,
			Set<String> compatibleTableExpressions) {
		super(
				canUseInnerJoins,
				navigablePath,
				modelPartContainer,
				cteTableReference.getIdentificationVariable(),
				sqlAliasBase,
				null
		);
		this.cteTableReference = cteTableReference;
		this.compatibleTableExpressions = compatibleTableExpressions;
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
