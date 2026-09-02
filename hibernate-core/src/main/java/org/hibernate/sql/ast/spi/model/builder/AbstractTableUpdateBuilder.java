/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdate;

import static org.hibernate.SPI.Role.IMPLEMENT;

/**
 * Base support for TableUpdateBuilder implementations
 *
 * @author Steve Ebersole
 */
@SPI( IMPLEMENT )
public abstract class AbstractTableUpdateBuilder<O extends MutationOperation>
		extends AbstractRestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>
		implements TableUpdateBuilder<O> {
	private final List<ColumnValueBinding> keyBindings = new ArrayList<>();
	private final List<ColumnValueBinding> valueBindings = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindings;

	private String sqlComment;

	public AbstractTableUpdateBuilder(
			MutationTarget mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableMapping, sessionFactory );
		this.sqlComment = "update for " + mutationTarget.getRolePath();
	}

	public AbstractTableUpdateBuilder(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.UPDATE, mutationTarget, tableReference, sessionFactory );
		this.sqlComment = "update for " + mutationTarget.getRolePath();
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
	public boolean hasAssignmentBindings() {
		return !valueBindings.isEmpty() || CollectionHelper.isNotEmpty( lobValueBindings );
	}

	@Override
	public void addColumnAssignment(ColumnValueBinding valueBinding) {
		if ( valueBinding.getColumnReference().getJdbcMapping().getJdbcType().isLob()
				&& getJdbcServices().getDialect().getLobSupport().forceLobAsLastValue() ) {
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
	public void addColumnAssignment(SelectableMapping columnMapping, String assignment) {
		addColumnAssignment( createValueBinding( assignment, columnMapping ) );
	}
}
