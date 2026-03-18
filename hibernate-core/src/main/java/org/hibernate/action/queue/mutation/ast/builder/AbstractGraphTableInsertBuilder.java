/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableInsert;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.ArrayList;
import java.util.List;

/// Base support for graph-based INSERT mutation builders.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractGraphTableInsertBuilder
		extends AbstractGraphTableMutationBuilder<TableInsert>
		implements GraphTableInsertBuilder {

	protected final List<ColumnValueBinding> keyBindingList = new ArrayList<>();
	protected final List<ColumnValueBinding> valueBindingList = new ArrayList<>();
	protected List<ColumnValueBinding> lobValueBindingList;

	private String sqlComment;

	protected AbstractGraphTableInsertBuilder(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super(MutationType.INSERT, mutationTarget, tableDescriptor, sessionFactory);
		this.sqlComment = "insert for " + mutationTarget.getRolePath();
	}

	protected AbstractGraphTableInsertBuilder(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super(MutationType.INSERT, mutationTarget, tableReference, sessionFactory);
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
	public void addValueColumn(ColumnValueBinding valueBinding) {
		valueBindingList.add( valueBinding );
	}

	@Override
	public void addValueColumn(ColumnDescriptor columnDescriptor) {
		final var binding = createValueBinding( columnDescriptor.writeFragment(), columnDescriptor );
		applyValueBinding( columnDescriptor.jdbcMapping(), binding );
	}

	@Override
	public void addValueColumn(SelectableMapping selectableMapping) {
		final var binding = createValueBinding( selectableMapping.getWriteExpression(), selectableMapping );
		applyValueBinding( selectableMapping.getJdbcMapping(),  binding );
	}

	@Override
	public void addValueColumn(String valueExpression, ColumnDescriptor columnDescriptor) {
		final var binding = createValueBinding( valueExpression, columnDescriptor );

		applyValueBinding( columnDescriptor.jdbcMapping(), binding );
	}

	private void applyValueBinding(JdbcMapping jdbcMapping, ColumnValueBinding binding) {
		// Handle LOB columns that need to be last
		// Check if JDBC type is LOB
		boolean isLob = isLobType(jdbcMapping);
		if (isLob && getJdbcServices().getDialect().forceLobAsLastValue()) {
			if (lobValueBindingList == null) {
				lobValueBindingList = new ArrayList<>();
			}
			lobValueBindingList.add(binding);
		}
		else {
			valueBindingList.add(binding);
		}
	}

	private boolean isLobType(org.hibernate.metamodel.mapping.JdbcMapping jdbcMapping) {
		final int jdbcType = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		return jdbcType == java.sql.Types.CLOB ||
			jdbcType == java.sql.Types.BLOB ||
			jdbcType == java.sql.Types.NCLOB;
	}

	@Override
	public void addValueColumn(String valueExpression, SelectableMapping selectableMapping) {
		final var binding = createValueBinding( valueExpression, selectableMapping );
		applyValueBinding( selectableMapping.getJdbcMapping(),  binding );
	}

	@Override
	public void addKeyColumn(ColumnDescriptor columnDescriptor) {
		final ColumnValueBinding binding = createValueBinding( columnDescriptor.writeFragment(), columnDescriptor );
		keyBindingList.add( binding );
	}

	@Override
	public void addKeyColumn(SelectableMapping selectableMapping) {
		final ColumnValueBinding binding = createValueBinding( selectableMapping.getWriteExpression(), selectableMapping );
		keyBindingList.add( binding );
	}

	@Override
	public void addKeyColumn(String columnWriteFragment, ColumnDescriptor columnDescriptor) {
		final ColumnValueBinding binding = createValueBinding( columnWriteFragment, columnDescriptor );
		keyBindingList.add( binding );
	}
}
