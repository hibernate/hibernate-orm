/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * Specialization of TableMutationBuilder for mutations which contain a
 * restriction.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractRestrictedTableMutationBuilder<O extends MutationOperation, M extends RestrictedTableMutation<O>>
		extends AbstractTableMutationBuilder<M>
		implements RestrictedTableMutationBuilder<O, M> {

	private final List<ColumnValueBinding> keyRestrictionBindings = new ArrayList<>();
	private List<ColumnValueBinding> optimisticLockBindings;

	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, table, sessionFactory );
	}

	public AbstractRestrictedTableMutationBuilder(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( mutationType, mutationTarget, tableReference, sessionFactory );
	}

	public List<ColumnValueBinding> getKeyRestrictionBindings() {
		return keyRestrictionBindings;
	}

	public List<ColumnValueBinding> getOptimisticLockBindings() {
		return optimisticLockBindings;
	}

	@Override
	public void addKeyRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		addColumn(
				columnName,
				columnWriteFragment,
				jdbcMapping,
				ParameterUsage.RESTRICT,
				keyRestrictionBindings
		);
	}

	@Override
	public void addNullOptimisticLockRestriction(SelectableMapping column) {
		if ( optimisticLockBindings == null ) {
			optimisticLockBindings = new ArrayList<>();
		}
		final ColumnReference columnReference = new ColumnReference(
				getMutatingTable(),
				column.getSelectionExpression(),
				column.getJdbcMapping()
		);
		optimisticLockBindings.add( new ColumnValueBinding( columnReference, null ) );
	}

	@Override
	public void addOptimisticLockRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping) {
		if ( optimisticLockBindings == null ) {
			optimisticLockBindings = new ArrayList<>();
		}

		addColumn( columnName, columnWriteFragment, jdbcMapping, ParameterUsage.RESTRICT, optimisticLockBindings );
	}

	@Override
	public void setWhere(String fragment) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void addWhereFragment(String fragment) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
