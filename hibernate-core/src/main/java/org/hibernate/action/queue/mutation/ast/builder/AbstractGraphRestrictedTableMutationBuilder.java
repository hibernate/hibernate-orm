/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.ColumnDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.meta.TableKeyDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableMutation;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.ArrayList;
import java.util.List;

/// Base support for graph-based mutations with restrictions (UPDATE, DELETE).
///
/// Handles key restrictions and optimistic lock restrictions.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractGraphRestrictedTableMutationBuilder<O extends JdbcOperation, M extends TableMutation<O>>
		extends AbstractGraphTableMutationBuilder<M> {

	protected final List<ColumnValueBinding> keyRestrictionBindings = new ArrayList<>();
	protected final List<ColumnValueBinding> optimisticLockBindings = new ArrayList<>();
	protected String whereFragment;

	protected AbstractGraphRestrictedTableMutationBuilder(
			MutationType mutationType,
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super(mutationType, mutationTarget, tableReference, sessionFactory);
	}

	protected AbstractGraphRestrictedTableMutationBuilder(
			MutationType mutationType,
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super(mutationType, mutationTarget, tableDescriptor, sessionFactory);
	}

	public List<ColumnValueBinding> getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	public List<ColumnValueBinding> getOptimisticLockBindings() {
		return optimisticLockBindings;
	}

	public void addKeyRestrictions(TableKeyDescriptor keyDescriptor) {
		// Use pre-normalized key columns from descriptor
		for (var keyColumn : keyDescriptor.columns()) {
			addKeyRestriction(keyColumn);
		}
	}

	public void addKeyRestriction(ColumnDescriptor columnDescriptor) {
		final var binding = createValueBinding( columnDescriptor.writeFragment(), columnDescriptor, ParameterUsage.RESTRICT);
		keyRestrictionBindings.add(binding);
	}

	public void addKeyRestriction(SelectableMapping selectableMapping) {
		final var binding = createValueBinding( selectableMapping.getWriteExpression(), selectableMapping, ParameterUsage.RESTRICT);
		keyRestrictionBindings.add(binding);
	}

	public void addOptimisticLockRestriction(ColumnDescriptor columnDescriptor) {
		final var binding = createValueBinding( columnDescriptor.writeFragment(), columnDescriptor, ParameterUsage.RESTRICT );
		optimisticLockBindings.add(binding);
	}

	public void addOptimisticLockRestriction(SelectableMapping selectableMapping) {
		final var binding = createValueBinding( selectableMapping.getWriteExpression(), selectableMapping, ParameterUsage.RESTRICT );
		optimisticLockBindings.add(binding);
	}

	public void addOptimisticLockRestriction(Object value, SelectableMapping jdbcValueMapping) {
		final ColumnValueBinding binding = value == null
				? createValueBinding( null, jdbcValueMapping, ParameterUsage.RESTRICT )
				: createValueBinding( jdbcValueMapping.getCustomWriteExpression(), jdbcValueMapping, ParameterUsage.RESTRICT );

		optimisticLockBindings.add(binding);
	}

	public void addNonKeyRestriction(ColumnValueBinding valueBinding) {
		optimisticLockBindings.add(valueBinding);
	}

	public void setWhere(String fragment) {
		this.whereFragment = fragment;
	}

	public void addWhereFragment(String fragment) {
		if ( whereFragment == null ) {
			whereFragment = fragment;
		}
		else {
			whereFragment = whereFragment + " and " + fragment;
		}
	}

	public String getWhereFragment() {
		return whereFragment;
	}
}
