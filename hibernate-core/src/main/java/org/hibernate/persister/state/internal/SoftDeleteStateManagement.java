/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.Internal;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.DeleteCoordinatorSoft;

/**
 * @author Gavin King
 */
@Internal
public final class SoftDeleteStateManagement extends AbstractStateManagement {
	public static final SoftDeleteStateManagement INSTANCE = new SoftDeleteStateManagement();

	private SoftDeleteStateManagement() {
	}

	@Override
	public DeleteCoordinator createDeleteCoordinator(EntityPersister persister) {
		return new DeleteCoordinatorSoft( persister, persister.getFactory() );
	}

}
