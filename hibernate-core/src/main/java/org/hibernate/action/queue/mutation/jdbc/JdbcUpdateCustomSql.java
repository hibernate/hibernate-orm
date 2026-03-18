/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/// Custom SQL UPDATE mutation using {@link EntityTableDescriptor}.
///
/// Parallel to custom SQL handling in legacy infrastructure but uses
/// pre-defined custom SQL from TableDescriptor.
///
/// @see org.hibernate.annotations.SQLUpdate
///
/// @author Steve Ebersole
@Incubating
public class JdbcUpdateCustomSql
		extends AbstractJdbcOperation
		implements JdbcUpdate, PreparableJdbcOperation {

	private final String customSql;
	private final boolean callable;
	private final List<ColumnValueParameter> parameters;

	public JdbcUpdateCustomSql(
			TableDescriptor tableDescriptor,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
				tableDescriptor,
				tableDescriptor.updateDetails(),
				mutationTarget,
				expectation,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				parameters
		);
	}

	public JdbcUpdateCustomSql(
			TableDescriptor tableDescriptor,
			TableMapping.MutationDetails mutationDetails,
			GraphMutationTarget<?> mutationTarget,
			Expectation expectation,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super(tableDescriptor, mutationTarget, expectation, parameters);
		this.customSql = mutationDetails.getCustomSql();
		this.callable = mutationDetails.isCallable();
		this.parameters = parameters;
		registerValueDescriptors(parameters);
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public String getSqlString() {
		return customSql;
	}

	@Override
	public boolean isCallable() {
		return callable;
	}

	@Override
	public String toString() {
		return "GraphJdbcUpdateCustomSql(" + getTableDescriptor().name() + ")";
	}
}
