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
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableInsert;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Base for provider-owned [TableInsertBuilder] implementations.
///
/// Extend this class to customize construction of an insert operation while
/// retaining standard parameter, assignment, and LOB-ordering behavior.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(IMPLEMENT)
public abstract class AbstractTableInsertBuilder
		extends AbstractTableMutationBuilder<TableInsert>
		implements TableInsertBuilder {
	private final List<ColumnValueBinding> valueBindingList = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindingList;

	private String sqlComment;

	/// Create an insert builder which owns a new reference to `table`.
	@SPI(IMPLEMENT)
	public AbstractTableInsertBuilder(
			MutationTarget mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.INSERT, mutationTarget, table, sessionFactory );
		this.sqlComment = "insert for " + mutationTarget.getRolePath();
	}

	/// Create an insert builder using the supplied mutating-table reference.
	@SPI(IMPLEMENT)
	public AbstractTableInsertBuilder(
			MutationTarget mutationTarget,
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

	protected List<ColumnValueBinding> getValueBindingList() {
		return valueBindingList;
	}

	protected List<ColumnValueBinding> getLobValueBindingList() {
		return lobValueBindingList;
	}

	@Override
	public void addColumnAssignment(ColumnValueBinding valueBinding) {
		if ( hasColumnAssignment( valueBinding ) ) {
			return;
		}
		if ( valueBinding.getColumnReference().getJdbcMapping().getJdbcType().isLob()
				&& getJdbcServices().getDialect().getLobSupport().forceLobAsLastValue() ) {
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
	public void addColumnAssignment(SelectableMapping columnMapping) {
		addColumnAssignment( columnMapping, columnMapping.getWriteExpression() );
	}

	@Override
	public void addColumnAssignment(SelectableMapping columnMapping, String assignment) {
		addColumnAssignment( createValueBinding( assignment, columnMapping ) );
	}


	@Override
	public boolean hasAssignmentBindings() {
		return !valueBindingList.isEmpty() || CollectionHelper.isNotEmpty( lobValueBindingList );
	}

	@Override
	public boolean hasColumnAssignment(SelectableMapping selectableMapping) {
		return valueBindingList.stream().anyMatch( binding -> binding.matches( selectableMapping ) )
			|| lobValueBindingList != null
					&& lobValueBindingList.stream().anyMatch( binding -> binding.matches( selectableMapping ) );
	}

	private boolean hasColumnAssignment(ColumnValueBinding valueBinding) {
		return valueBindingList.stream().anyMatch( binding -> binding.equals( valueBinding ) )
			|| lobValueBindingList != null
					&& lobValueBindingList.stream().anyMatch( binding -> binding.equals( valueBinding ) );
	}
}
