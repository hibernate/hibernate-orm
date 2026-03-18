/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdate;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdateCustomSql;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping.MutationDetails;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Custom SQL UPDATE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to {@link org.hibernate.sql.model.internal.TableUpdateCustomSql}
/// but works with graph-based infrastructure.
///
/// @see org.hibernate.annotations.SQLUpdate
///
/// @author Steve Ebersole
@Incubating
public class TableUpdateCustomSql
		extends AbstractAssigningTableMutation<JdbcUpdate>
		implements TableUpdate, CustomSqlMutation {

	private final List<ColumnValueBinding> keyRestrictionBindings;
	private final List<ColumnValueBinding> optLockRestrictionBindings;
	private final MutationDetails mutationDetails;

	public TableUpdateCustomSql(
			TableDescriptor tableDescriptor,
			MutationDetails mutationDetails,
			GraphMutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(
			tableDescriptor,
			MutationType.UPDATE,
			mutationTarget,
			sqlComment,
			parameters,
			valueBindings
		);
		this.mutationDetails = mutationDetails;
		this.keyRestrictionBindings = keyRestrictionBindings;
		this.optLockRestrictionBindings = optLockRestrictionBindings;
	}

	private static List<ColumnValueBinding> combineBindings(
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		final var combined = new java.util.ArrayList<ColumnValueBinding>();
		if (valueBindings != null) {
			combined.addAll(valueBindings);
		}
		if (keyRestrictionBindings != null) {
			combined.addAll(keyRestrictionBindings);
		}
		if (optLockRestrictionBindings != null) {
			combined.addAll(optLockRestrictionBindings);
		}
		return combined;
	}

	@Override
	protected String getLoggableName() {
		return "GraphTableUpdateCustomSql";
	}

	@Override
	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	@Override
	public List<ColumnValueBinding> getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	@Override
	public List<ColumnValueBinding> getOptLockRestrictionBindings() {
		return optLockRestrictionBindings;
	}

	@Override
	public String getCustomSql() {
		return mutationDetails.getCustomSql();
	}

	@Override
	public boolean isCallable() {
		return mutationDetails.isCallable();
	}

	@Override
	public JdbcUpdate createMutationOperation() {
		return new JdbcUpdateCustomSql(
			getTableDescriptor(),
			getMutationTarget(),
			getTableDescriptor().updateDetails().getExpectation(),
			valueBindings,
			keyRestrictionBindings,
			optLockRestrictionBindings,
			getParameters()
		);
	}
}
