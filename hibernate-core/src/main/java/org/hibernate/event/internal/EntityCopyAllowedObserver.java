/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventSource;
import jakarta.annotation.Nonnull;

/**
 * An {@link EntityCopyObserver} implementation that allows multiple representations of
 * the same persistent entity to be merged.
 *
 * @author Gail Badner
 */
public final class EntityCopyAllowedObserver implements EntityCopyObserver {

	public static final String SHORT_NAME = "allow";
	private static final EntityCopyObserver INSTANCE = new EntityCopyAllowedObserver();

	//This implementation of EntityCopyObserver is stateless, so no need to create multiple copies:
	public static final EntityCopyObserverFactory FACTORY_OF_SELF = () -> INSTANCE;

	private EntityCopyAllowedObserver() {
		//Not to be constructed; use INSTANCE.
	}

	@Override
	public void entityCopyDetected(
			@Nonnull Object managedEntity,
			@Nonnull Object mergeEntity1,
			@Nonnull Object mergeEntity2,
			@Nonnull EventSource session) {
		// do nothing.
	}

	public void clear() {
		// do nothing.
	}

	@Override
	public void topLevelMergeComplete(@Nonnull EventSource session) {
		// do nothing.
	}

}
