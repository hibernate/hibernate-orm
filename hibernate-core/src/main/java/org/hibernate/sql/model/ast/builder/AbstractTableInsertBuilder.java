/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;

/**
 * Base support for TableInsertBuilder implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableInsertBuilder
		extends AbstractTableMutationBuilder<TableInsert>
		implements TableInsertBuilder {
	private final List<ColumnValueBinding> keyBindingList = new ArrayList<>();
	private final List<ColumnValueBinding> valueBindingList = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindingList;

	private String sqlComment;

	public AbstractTableInsertBuilder(
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.INSERT, mutationTarget, table, sessionFactory );
		this.sqlComment = "insert for " + mutationTarget.getRolePath();
	}

	public AbstractTableInsertBuilder(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.INSERT, mutationTarget, tableReference, sessionFactory );
		this.sqlComment = "insert for " + mutationTarget.getRolePath();
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
	}

	protected List<ColumnValueBinding> getKeyBindingList() {
		return keyBindingList;
	}

	protected List<ColumnValueBinding> getValueBindingList() {
		return valueBindingList;
	}

	protected List<ColumnValueBinding> getLobValueBindingList() {
		return lobValueBindingList;
	}

	@Override
	public void addValueColumn(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping, boolean isLob) {
		final ColumnValueBinding valueBinding = createValueBinding( columnName, columnWriteFragment, jdbcMapping );

		if ( isLob && getJdbcServices().getDialect().forceLobAsLastValue() ) {
			if ( lobValueBindingList == null ) {
				lobValueBindingList = new ArrayList<>();
			}
			lobValueBindingList.add( valueBinding );
		}
		else {
			valueBindingList.add( valueBinding );
		}
	}

	@Override
	public void addValueColumn(ColumnValueBinding valueBinding) {
		valueBindingList.add( valueBinding );
	}

	@Override
	public void addKeyColumn(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		addColumn( columnName, columnWriteFragment, jdbcMapping, keyBindingList );
	}
}
