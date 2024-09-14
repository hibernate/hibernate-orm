/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableDelete;

/**
 * @author Steve Ebersole
 */
public class TableDeleteBuilderSkipped implements TableDeleteBuilder {
	private final MutatingTableReference tableReference;

	public TableDeleteBuilderSkipped(TableMapping tableMapping) {
		tableReference = new MutatingTableReference( tableMapping );
	}

	@Override
	public void addKeyRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
	}

	@Override
	public void addOptimisticLockRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
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
}
