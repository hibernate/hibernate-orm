/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal.lock;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
class SimpleTableGroup implements TableGroup {
	private final TableReference tableReference;
	private final String tableName;
	private final EntityMappingType entityMappingType;
	private final NavigablePath navigablePath;

	public SimpleTableGroup(TableReference tableReference, String tableName, EntityMappingType entityMappingType) {
		this.tableReference = tableReference;
		this.tableName = tableName;
		this.entityMappingType = entityMappingType;
		this.navigablePath = new NavigablePath( tableName );
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
		return entityMappingType;
	}

	@Override
	public String getSourceAlias() {
		return "";
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return List.of();
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return List.of();
	}

	@Override
	public boolean canUseInnerJoins() {
		return false;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
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
		return entityMappingType;
	}

	@Override
	public TableReference getTableReference(NavigablePath navigablePath, String tableExpression, boolean resolve) {
		if ( tableName.equals( tableExpression ) ) {
			return tableReference;
		}
		return null;
	}
}
