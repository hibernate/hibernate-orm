/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The default implementation of {@link CustomEntityDirtinessStrategy} which does nada.
 *
 * @author Steve Ebersole
 */
public class DefaultCustomEntityDirtinessStrategy implements CustomEntityDirtinessStrategy {
	public static final DefaultCustomEntityDirtinessStrategy INSTANCE = new DefaultCustomEntityDirtinessStrategy();

	@Override
	public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
		return false;
	}

	@Override
	public boolean isDirty(Object entity, EntityPersister persister, Session session) {
		return false;
	}

	@Override
	public void resetDirty(Object entity, EntityPersister persister, Session session) {
	}

	@Override
	public void findDirty(
			Object entity,
			EntityPersister persister,
			Session session,
			DirtyCheckContext dirtyCheckContext) {
	}
}
