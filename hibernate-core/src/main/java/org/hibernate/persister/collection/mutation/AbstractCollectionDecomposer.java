/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.exec.ExecutionContext;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionDecomposer implements CollectionDecomposer {

	protected void preUpdate(CollectionUpdateAction action, SharedSessionContractImplementor session) {
		action.preUpdate();
	}

	protected Object lockCacheItem(CollectionAction action, SharedSessionContractImplementor session) {
		if (!action.getPersister().hasCache()) {
			return null;
		}

		final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
		return cache.generateCacheKey(
				action.getKey(),
				action.getPersister(),
				session.getFactory(),
				session.getTenantIdentifier()
		);
		// Note: The actual lock is obtained in CollectionAction.beforeExecutions()
		// We just generate the cache key here for use in post-execution
	}

	protected static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final BasicCollectionPersister mutationTarget;

		public RemoveBindPlan(Object key, BasicCollectionPersister mutationTarget) {
			this.key = key;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void execute(
				ExecutionContext context,
				PlannedOperation plannedOperation,
				SharedSessionContractImplementor session) {
			context.executeRow(
					plannedOperation,
					valueBindings -> {
						var fkDescriptor = mutationTarget.getAttributeMapping().getKeyDescriptor();
						fkDescriptor.getKeyPart().decompose(
								key,
								(valueIndex, value, jdbcValueMapping) -> {
									valueBindings.bindValue(
											value,
											jdbcValueMapping.getSelectableName(),
											ParameterUsage.RESTRICT
									);
								},
								session
						);
					},
					null
			);
		}
	}
}
