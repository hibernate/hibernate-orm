/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.internal;

import java.util.List;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.AbstractTableUpdate;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * @author Steve Ebersole
 */
public class TableUpdateStandard extends AbstractTableUpdate<JdbcMutationOperation> {
	private final String whereFragment;

	public TableUpdateStandard(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this( mutatingTable, mutationTarget, parameters, valueBindings, keyRestrictionBindings, optLockRestrictionBindings, null );
	}

	public TableUpdateStandard(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			String whereFragment) {
		super( mutatingTable, mutationTarget, parameters, valueBindings, keyRestrictionBindings, optLockRestrictionBindings );
		this.whereFragment = whereFragment;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	public String getWhereFragment() {
		return whereFragment;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitStandardTableUpdate( this );
	}
}
