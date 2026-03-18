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
import org.hibernate.action.queue.mutation.ast.TableUpdate;
import org.hibernate.action.queue.mutation.ast.TableUpdateCustomSql;
import org.hibernate.action.queue.mutation.ast.TableUpdateStandard;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Standard builder for graph-based UPDATE mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard}
/// but uses {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// @author Steve Ebersole
@Incubating
public class GraphTableUpdateBuilderStandard
		extends AbstractGraphTableUpdateBuilder
		implements GraphTableUpdateBuilder {

	private final String whereFragment;
	private final boolean isCustomSql;

	public GraphTableUpdateBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableReference, null, sessionFactory );
	}

	public GraphTableUpdateBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableReference, sessionFactory);
		this.whereFragment = whereFragment;
		this.isCustomSql = tableReference.tableDescriptor().updateDetails() != null
				&& tableReference.tableDescriptor().updateDetails().getCustomSql() != null;
	}

	public GraphTableUpdateBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableDescriptor, null, sessionFactory );
	}

	public GraphTableUpdateBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableDescriptor, sessionFactory);
		this.whereFragment = whereFragment;
		this.isCustomSql = tableDescriptor.updateDetails() != null
				&& tableDescriptor.updateDetails().getCustomSql() != null;
	}

	@Override
	public TableUpdate buildMutation() {
		if (isCustomSql) {
			return new TableUpdateCustomSql(
					getTableDescriptor(),
					getTableDescriptor().updateDetails(),
					getMutationTarget(),
					getSqlComment(),
					valueBindings,
					keyRestrictionBindings,
					optimisticLockBindings,
					getParameters()
			);
		}

		return new TableUpdateStandard(
				getTableDescriptor(),
				getMutationTarget(),
				getSqlComment(),
				valueBindings,
				keyRestrictionBindings,
				optimisticLockBindings,
				getParameters()
		);
	}
}
