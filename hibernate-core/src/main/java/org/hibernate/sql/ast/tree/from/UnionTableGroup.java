/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.NavigablePath;

/**
 * @author Andrea Boriero
 */
public class UnionTableGroup implements VirtualTableGroup {
	private final boolean canUseInnerJoins;
	private final NavigablePath navigablePath;
	private List<TableGroupJoin> tableGroupJoins;

	private final UnionSubclassEntityPersister modelPart;
	private final String sourceAlias;
	private final TableReference tableReference;

	public UnionTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			TableReference tableReference,
			UnionSubclassEntityPersister modelPart,
			String sourceAlias) {
		this.canUseInnerJoins = canUseInnerJoins;
		this.navigablePath = navigablePath;
		this.tableReference = tableReference;
		this.modelPart = modelPart;
		this.sourceAlias = sourceAlias;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ModelPart getExpressionType() {
		return getModelPart();
	}

	@Override
	public String getGroupAlias() {
		return null;
	}

	@Override
	public ModelPartContainer getModelPart() {
		return modelPart;
	}

	@Override
	public String getSourceAlias() {
		return sourceAlias;
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null ? Collections.emptyList() : Collections.unmodifiableList( tableGroupJoins );
	}

	@Override
	public boolean hasTableGroupJoins() {
		return tableGroupJoins != null && !tableGroupJoins.isEmpty();
	}

	@Override
	public boolean canUseInnerJoins() {
		return canUseInnerJoins;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( tableGroupJoins == null ) {
			tableGroupJoins = new ArrayList<>();
		}
		if ( !tableGroupJoins.contains( join ) ) {
			tableGroupJoins.add( join );
		}
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins != null ) {
			tableGroupJoins.forEach( consumer );
		}
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return tableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInnerJoinPossible() {
		return false;
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression) {
		return resolveTableReference( navigablePath, tableExpression );
	}

	@Override
	public TableReference resolveTableReference(NavigablePath navigablePath, String tableExpression) {
		if ( tableReference.getTableReference( navigablePath, tableExpression ) != null ) {
			return tableReference;
		}
		if ( tableGroupJoins != null ) {
			for ( TableGroupJoin tableGroupJoin : tableGroupJoins ) {
				final TableReference tableReference = tableGroupJoin.getJoinedGroup()
						.resolveTableReference( navigablePath, tableExpression );
				if ( tableReference != null ) {
					return tableReference;
				}
			}
		}
		return null;
	}
}
