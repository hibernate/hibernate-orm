/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueBindingList;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Base for provider-owned table-mutation builders which collect key and
/// optimistic-lock restrictions.
///
/// Extend this class for update or delete forms whose command restricts the
/// affected rows. Choose the constructor accepting a [TableMapping] when the
/// builder should create the mutating table reference, or supply an existing
/// [MutatingTableReference] when reference identity must be preserved.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(IMPLEMENT)
public abstract class AbstractRestrictedTableMutationBuilder<O extends MutationOperation, M extends RestrictedTableMutation<O>>
		extends AbstractTableMutationBuilder<M>
		implements RestrictedTableMutationBuilder<O, M> {

	private final ColumnValueBindingList keyRestrictionBindings;
	private final ColumnValueBindingList optimisticLockBindings;

	/// Create a restricted builder which owns a new reference to `table`.
	@SPI(IMPLEMENT)
	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, table, sessionFactory );
		this.keyRestrictionBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
		this.optimisticLockBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
	}

	/// Create a restricted builder using the supplied mutating-table reference.
	@SPI(IMPLEMENT)
	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, tableReference, sessionFactory );
		this.keyRestrictionBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
		this.optimisticLockBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
	}

	@Override
	public ColumnValueBindingList getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	@Override
	public ColumnValueBindingList getOptimisticLockBindings() {
		return optimisticLockBindings;
	}

	@Override
	public void addNonKeyRestriction(ColumnValueBinding valueBinding) {
		optimisticLockBindings.addRestriction( valueBinding );
	}

	@Override
	public void addNonKeyRestriction(SelectableMapping restrictableMapping, String restrictionExpression) {
		optimisticLockBindings.addRestriction( ColumnValueBindingBuilder.createValueBinding(
				restrictionExpression,
				restrictableMapping,
				getMutatingTable(),
				ParameterUsage.RESTRICT,
				getParameters()::apply
		) );
	}

	@Override
	public void addKeyRestrictionBinding(SelectableMapping selectableMapping) {
		keyRestrictionBindings.addRestriction( selectableMapping );
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
		optimisticLockBindings.addNullRestriction( column );
	}

	@Override
	public void addOptimisticLockRestriction(SelectableMapping selectableMapping) {
		optimisticLockBindings.addRestriction( selectableMapping );
	}

	@Override
	public void setWhere(String fragment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addWhereFragment(String fragment) {
		throw new UnsupportedOperationException();
	}
}
