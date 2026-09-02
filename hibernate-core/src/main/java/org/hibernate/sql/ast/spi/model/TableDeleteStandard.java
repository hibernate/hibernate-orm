/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import java.util.List;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.jdbc.JdbcDeleteMutation;

/**
 * @author Steve Ebersole
 */
public class TableDeleteStandard extends AbstractTableDelete implements GeneratedMutation<JdbcDeleteMutation> {
	private final String whereFragment;

	public TableDeleteStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this( mutatingTable, mutationTarget, sqlComment, keyRestrictionBindings, optLockRestrictionBindings, parameters, null );
	}

	public TableDeleteStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters,
			String whereFragment) {
		super( mutatingTable, mutationTarget, sqlComment, keyRestrictionBindings, optLockRestrictionBindings, parameters );
		this.whereFragment = whereFragment;
	}

	public String getWhereFragment() {
		return whereFragment;
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitStandardTableDelete( this );
	}
}
