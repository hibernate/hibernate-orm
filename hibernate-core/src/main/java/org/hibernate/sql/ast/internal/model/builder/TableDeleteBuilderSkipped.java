/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal.model.builder;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilder;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueBindingList;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableDelete;

/**
 * @author Steve Ebersole
 */
public class TableDeleteBuilderSkipped implements TableDeleteBuilder {
	private final MutatingTableReference tableReference;

	public TableDeleteBuilderSkipped(TableMapping tableMapping) {
		tableReference = new MutatingTableReference( tableMapping );
	}

	@Override
	public void addNonKeyRestriction(ColumnValueBinding valueBinding) {
	}

	@Override
	public void addNonKeyRestriction(SelectableMapping restrictableMapping, String restrictionExpression) {
	}

	@Override
	public void addKeyRestrictionBinding(SelectableMapping selectableMapping) {
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
	}

	@Override
	public void addOptimisticLockRestriction(SelectableMapping selectableMapping) {
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
	public void setWhere(String fragment) {
	}

	@Override
	public void addWhereFragment(String fragment) {
	}

	@Override
	public MutatingTableReference getMutatingTable() {
		return tableReference;
	}

	@Override
	public TableDelete buildMutation() {
		return null;
	}

	@Override
	public boolean hasValueBindings() {
		return false;
	}
}
