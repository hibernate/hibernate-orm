/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standalone legacy audit writer for entity audit rows.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class AuditMutationWriter extends AbstractAuditCoordinator {
	public AuditMutationWriter(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}
}
