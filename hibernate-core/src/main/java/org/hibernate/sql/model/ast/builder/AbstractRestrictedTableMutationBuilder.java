/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.TenantIdColumnValueBinding;

/**
 * Specialization of TableMutationBuilder for mutations which contain a
 * restriction.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractRestrictedTableMutationBuilder<O extends MutationOperation, M extends RestrictedTableMutation<O>>
		extends AbstractTableMutationBuilder<M>
		implements RestrictedTableMutationBuilder<O, M> {

	private final ColumnValueBindingList keyRestrictionBindings;
	private final ColumnValueBindingList optimisticLockBindings;

	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, table, sessionFactory );
		this.keyRestrictionBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
		this.optimisticLockBindings = new ColumnValueBindingList( getMutatingTable(), getParameters(), ParameterUsage.RESTRICT );
	}

	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
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

	/**
	 * Custom SQL may use the original parameter list or append the session tenant.
	 * Remove the tenant descriptor as well as its restriction when it is omitted.
	 * The count cannot identify omitted or reordered non-tenant parameters:
	 * the custom SQL must preserve the original parameter list and its order.
	 */
	protected void adjustCustomSqlTenantRestriction(TableMapping.MutationDetails details, List<ColumnValueParameter> parameters) {
		if ( optimisticLockBindings.stream().anyMatch( binding -> binding instanceof TenantIdColumnValueBinding ) ) {
			final int expected = parameters.size() + details.getExpectation().getNumberOfParametersUsed();
			final int actual = details.getCustomSqlParameterCount();
			if ( actual == expected - 1 ) {
				optimisticLockBindings.removeIf( binding -> binding instanceof TenantIdColumnValueBinding );
				getParameters().removeIf( parameter -> parameter.getUsage() == ParameterUsage.TENANT );
				parameters.removeIf( parameter -> parameter.getUsage() == ParameterUsage.TENANT );
			}
			else if ( actual != expected ) {
				throw new MappingException(
						"Custom SQL " + details.getMutationType() + " for '" + getMutationTarget().getRolePath()
						+ "' on table '" + getMutatingTable().getTableName() + "' has " + actual + " JDBC parameters, but expected "
						+ (expected - 1) + " without the tenant id or " + expected + " with the tenant id as the last parameter" );
			}
		}
	}

	@Override
	public void addNonKeyRestriction(ColumnValueBinding valueBinding) {
		optimisticLockBindings.addRestriction( valueBinding );
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
