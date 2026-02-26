/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Merge coordinator for audited entities.
 */
public class MergeCoordinatorAudit extends UpdateCoordinatorAudit {
	public MergeCoordinatorAudit(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory, currentUpdateCoordinator );
	}
}
