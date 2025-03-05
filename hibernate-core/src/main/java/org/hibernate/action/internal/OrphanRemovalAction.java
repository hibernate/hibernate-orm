/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

public final class OrphanRemovalAction extends EntityDeleteAction {

	public OrphanRemovalAction(
			Object id, Object[] state, Object version, Object instance,
			EntityPersister persister, boolean isCascadeDeleteEnabled, EventSource session) {
		super( id, state, version, instance, persister, isCascadeDeleteEnabled, session );
	}
}
