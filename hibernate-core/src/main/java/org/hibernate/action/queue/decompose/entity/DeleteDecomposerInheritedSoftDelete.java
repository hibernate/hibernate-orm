/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.TableMutation;

import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class DeleteDecomposerInheritedSoftDelete extends AbstractDeleteDecomposer {
	public DeleteDecomposerInheritedSoftDelete(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );
		assert entityPersister.getSoftDeleteMapping() != null;
		assert entityPersister.getRootEntityDescriptor() != entityPersister;
	}

	@Override
	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return Map.of();
	}

	@Override
	public List<PlannedOperation> decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext) {
		return entityPersister.getRootEntityDescriptor()
				.getEntityPersister()
				.getDeleteDecomposer()
				.decompose( action, ordinalBase, session, decompositionContext );
	}
}
