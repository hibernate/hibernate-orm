/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * @author Steve Ebersole
 */
public class TableUpdateBuilderSkipped implements TableUpdateBuilder {
	private final MutatingTableReference tableReference;

	public TableUpdateBuilderSkipped(MutatingTableReference tableReference) {
		this.tableReference = tableReference;
	}

	@Override
	public MutatingTableReference getMutatingTable() {
		return tableReference;
	}

	@Override
	public LogicalTableUpdate<JdbcMutationOperation> buildMutation() {
		return null;
	}

	@Override
	public void addNonKeyRestriction(ColumnValueBinding valueBinding) {
		// nothing to do
	}

	@Override
	public void addNonKeyRestriction(SelectableMapping restrictableMapping, String restrictionExpression) {
		// nothing to do
	}

	@Override
	public void addKeyRestrictionBinding(SelectableMapping selectableMapping) {
		// nothing to do
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
		// nothing to do
	}

	@Override
	public void addOptimisticLockRestriction(SelectableMapping selectableMapping) {
		// nothing to do
	}

	@Override
	public ColumnValueBindingList getKeyRestrictionBindings() {
		return null;
	}

	@Override
	public ColumnValueBindingList getOptimisticLockBindings() {
		return null;
	}

	@Override
	public void addWhereFragment(String fragment) {
		// nothing to do
	}

	@Override
	public void addColumnAssignment(SelectableMapping columnMapping, String assignment) {
		// nothing to do
	}

	@Override
	public void addColumnAssignment(ColumnValueBinding valueBinding) {
		// nothing to do
	}

	@Override
	public void setWhere(String fragment) {
		// nothing to do
	}

	@Override
	public boolean hasAssignmentBindings() {
		return false;
	}
}
