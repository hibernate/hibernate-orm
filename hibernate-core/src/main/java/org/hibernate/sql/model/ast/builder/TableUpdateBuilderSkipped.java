/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
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
	public RestrictedTableMutation<JdbcMutationOperation> buildMutation() {
		return null;
	}

	@Override
	public void addKeyRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		// nothing to do
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
		// nothing to do
	}

	@Override
	public void addOptimisticLockRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		// nothing to do
	}

	@Override
	public void addLiteralRestriction(String columnName, String sqlLiteralText, JdbcMapping jdbcMapping) {
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
	public void addValueColumn(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping, boolean isLob) {
		// nothing to do
	}

	@Override
	public void addKeyColumn(String columnName, String valueExpression, JdbcMapping jdbcMapping) {
		// nothing to do
	}

	@Override
	public void setWhere(String fragment) {
		// nothing to do
	}
}
