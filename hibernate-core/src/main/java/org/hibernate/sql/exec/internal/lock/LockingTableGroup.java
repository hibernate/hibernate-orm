/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * TableGroup wrapping a {@linkplain TableLock table to be locked}.
 *
 * @author Steve Ebersole
 */
public class LockingTableGroup implements TableGroup {
	private final TableReference tableReference;
	private final String tableName;
	private final ModelPartContainer modelPart;
	private final NavigablePath navigablePath;
	private final SelectableMappings keyColumnMappings;

	private List<TableGroupJoin> tableGroupJoins;

	public LockingTableGroup(
			TableReference tableReference,
			String tableName,
			ModelPartContainer modelPart,
			SelectableMappings keyColumnMappings) {
		this.tableReference = tableReference;
		this.tableName = tableName;
		this.modelPart = modelPart;
		this.navigablePath = new NavigablePath( tableName );
		this.keyColumnMappings = keyColumnMappings;
	}

	public SelectableMappings getKeyColumnMappings() {
		return keyColumnMappings;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getGroupAlias() {
		return "";
	}

	@Override
	public ModelPartContainer getModelPart() {
		return modelPart;
	}

	@Override
	public String getSourceAlias() {
		return "";
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return tableGroupJoins == null
				? Collections.emptyList()
				: tableGroupJoins;
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return List.of();
	}

	@Override
	public boolean canUseInnerJoins() {
		return true;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		if ( join.getJoinedGroup().isRealTableGroup() ) {
			throw new UnsupportedOperationException();
		}

		if ( tableGroupJoins == null ) {
			tableGroupJoins = new ArrayList<>();
		}
		tableGroupJoins.add( join );
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		addTableGroupJoin( join );
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
		if ( tableGroupJoins != null ) {
			tableGroupJoins.forEach( consumer );
		}
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( tableName );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return tableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return List.of();
	}

	@Override
	public ModelPart getExpressionType() {
		return modelPart;
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression, boolean resolve) {
		if ( tableName.equals( tableExpression ) ) {
			return tableReference;
		}
		return null;
	}
}
