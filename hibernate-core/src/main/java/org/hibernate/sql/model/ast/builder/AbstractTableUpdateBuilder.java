/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TableUpdate;

/**
 * Base support for TableUpdateBuilder implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableUpdateBuilder<O extends MutationOperation>
		extends AbstractRestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>
		implements TableUpdateBuilder<O> {
	private final List<ColumnValueBinding> keyBindings = new ArrayList<>();
	private final List<ColumnValueBinding> valueBindings = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindings;

	private String sqlComment;

	public AbstractTableUpdateBuilder(
			MutationTarget<?> mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableMapping, sessionFactory );
		this.sqlComment = "update for " + mutationTarget.getRolePath();
	}

	public AbstractTableUpdateBuilder(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableReference, sessionFactory );
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
	}

	/**
	 * The bindings for each key restriction (WHERE clause).
	 *
	 * @see TableUpdate#getKeyBindings
	 */
	protected List<ColumnValueBinding> getKeyBindings() {
		return keyBindings;
	}

	/**
	 * The (non-LOB) bindings for each column being updated (SET clause)
	 *
	 * @see TableUpdate#getValueBindings
	 */
	protected List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	/**
	 * @apiNote The distinction with {@link #getValueBindings} is to help
	 * in cases e.g. where a dialect needs to order all LOB bindings after
	 * all non-LOB bindings
	 *
	 * @see TableUpdate#getValueBindings
	 */
	protected List<ColumnValueBinding> getLobValueBindings() {
		return lobValueBindings;
	}

	@Override
	public void addValueColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping,
			boolean isLob) {
		final ColumnValueBinding valueBinding = createValueBinding( columnName, columnWriteFragment, jdbcMapping );

		if ( isLob && getJdbcServices().getDialect().forceLobAsLastValue() ) {
			if ( lobValueBindings == null ) {
				lobValueBindings = new ArrayList<>();
			}
			lobValueBindings.add( valueBinding );
		}
		else {
			valueBindings.add( valueBinding );
		}
	}

	@Override
	public void addValueColumn(ColumnValueBinding valueBinding) {
		valueBindings.add( valueBinding );
	}

	@Override
	public void addKeyColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping) {
		addColumn( columnName, columnWriteFragment, jdbcMapping, keyBindings );
	}
}
