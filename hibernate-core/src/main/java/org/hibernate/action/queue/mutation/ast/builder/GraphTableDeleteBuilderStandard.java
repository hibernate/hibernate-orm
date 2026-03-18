/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableDelete;
import org.hibernate.action.queue.mutation.ast.TableDeleteCustomSql;
import org.hibernate.action.queue.mutation.ast.TableDeleteStandard;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Standard builder for graph-based DELETE mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard}
/// but uses {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// @author Steve Ebersole
@Incubating
public class GraphTableDeleteBuilderStandard
		extends AbstractGraphTableDeleteBuilder {

	private final String whereFragment;
	private final boolean isCustomSql;

	public GraphTableDeleteBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableDescriptor, null, sessionFactory );
	}

	public GraphTableDeleteBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableDescriptor, sessionFactory);
		this.whereFragment = whereFragment;
		this.isCustomSql = tableDescriptor.deleteDetails() != null
				&& tableDescriptor.deleteDetails().getCustomSql() != null;
	}

	public GraphTableDeleteBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableReference, null, sessionFactory );
	}

	public GraphTableDeleteBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableReference, sessionFactory);
		this.whereFragment = whereFragment;
		this.isCustomSql = tableReference.tableDescriptor().deleteDetails() != null
				&& tableReference.tableDescriptor().deleteDetails().getCustomSql() != null;
	}

	@Override
	public TableDelete buildMutation() {
		if (isCustomSql) {
			return new TableDeleteCustomSql(
					getTableDescriptor(),
					getMutationTarget(),
					getSqlComment(),
					keyRestrictionBindings,
					optimisticLockBindings,
					getParameters()
			);
		}

		return new TableDeleteStandard(
				getTableDescriptor(),
				getMutationTarget(),
				getSqlComment(),
				keyRestrictionBindings,
				optimisticLockBindings,
				getParameters()
		);
	}
}
