/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableUpdate;
import org.hibernate.action.queue.mutation.ast.TableUpdateCustomSql;
import org.hibernate.action.queue.mutation.ast.TableUpdateStandard;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.mutation.CollectionGraphMutationTarget;


/**
 * Custom table update builder for one-to-many collections that handles row deletes
 *
 * @author Marco Belladelli
 */
public class CollectionRowDeleteByUpdateSetNullBuilder
		extends GraphTableUpdateBuilderStandard {
	public CollectionRowDeleteByUpdateSetNullBuilder(
			CollectionGraphMutationTarget mutationTarget,
			MutatingTableReference tableReference,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, whereFragment, sessionFactory );
	}

	public CollectionRowDeleteByUpdateSetNullBuilder(
			CollectionGraphMutationTarget mutationTarget,
			TableDescriptor tableDescriptor,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableDescriptor, whereFragment, sessionFactory );
	}

	public CollectionRowDeleteByUpdateSetNullBuilder(
			CollectionGraphMutationTarget  mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableReference, null, sessionFactory );
	}

	@Override
	protected CollectionGraphMutationTarget getMutationTarget() {
		return (CollectionGraphMutationTarget) super.getMutationTarget();
	}

	@Override
	public TableUpdate buildMutation() {
		final var tableDescriptor = tableReference.tableDescriptor();
		if ( tableDescriptor.deleteDetails().getCustomSql() != null ) {
			return new TableUpdateCustomSql(
					tableReference.tableDescriptor(),
					tableDescriptor.deleteDetails(),
					getMutationTarget(),
					getSqlComment(),
					valueBindings,
					getKeyRestrictionBindings(),
					getOptimisticLockBindings(),
					getParameters()
			);
		}
		return new TableUpdateStandard(
				tableReference.tableDescriptor(),
				getMutationTarget(),
				getSqlComment(),
				valueBindings,
				getKeyRestrictionBindings(),
				getOptimisticLockBindings(),
				getParameters()
		);
	}
}
