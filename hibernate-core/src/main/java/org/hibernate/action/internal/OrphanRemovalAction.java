/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

public final class OrphanRemovalAction extends EntityDeleteAction {

	public OrphanRemovalAction(
			@Nonnull Object id,
			@Nullable Object[] state,
			@Nullable Object version,
			@Nonnull Object instance,
			@Nonnull EntityPersister persister,
			boolean isCascadeDeleteEnabled,
			@Nonnull EventSource session) {
		super( id, state, version, instance, persister, isCascadeDeleteEnabled, session );
	}
}
