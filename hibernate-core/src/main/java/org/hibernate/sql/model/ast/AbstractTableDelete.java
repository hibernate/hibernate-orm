/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast;

import java.util.List;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableDelete extends AbstractRestrictedTableMutation<JdbcDeleteMutation> implements TableDelete {

	public AbstractTableDelete(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
				mutatingTable,
				mutationTarget,
				"delete for " + mutationTarget.getRolePath(),
				keyRestrictionBindings,
				optLockRestrictionBindings,
				parameters
		);
	}

	public AbstractTableDelete(
			MutatingTableReference mutatingTable,
			MutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		super( mutatingTable, mutationTarget, sqlComment, keyRestrictionBindings, optLockRestrictionBindings, parameters );
	}

	@Override
	protected String getLoggableName() {
		return "TableDelete";
	}

	@Override
	public Expectation getExpectation() {
		return getMutatingTable().getTableMapping().getDeleteDetails().getExpectation();
	}

	@Override
	protected JdbcDeleteMutation createMutationOperation(
			TableMapping tableDetails,
			String sql,
			List<JdbcParameterBinder> effectiveBinders) {
		return new JdbcDeleteMutation(
				tableDetails,
				getMutationTarget(),
				sql,
				isCallable(),
				getExpectation(),
				effectiveBinders
		);
	}
}
