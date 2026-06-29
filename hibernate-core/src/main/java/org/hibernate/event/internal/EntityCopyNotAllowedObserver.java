/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventSource;

import static org.hibernate.pretty.MessageHelper.infoString;
import jakarta.annotation.Nonnull;

/**
 * @author Gail Badner
 */
public final class EntityCopyNotAllowedObserver implements EntityCopyObserver {

	public static final String SHORT_NAME = "disallow";
	private static final EntityCopyNotAllowedObserver INSTANCE = new EntityCopyNotAllowedObserver();
	//This implementation of EntityCopyObserver is stateless, so no need to create multiple copies:
	public static final EntityCopyObserverFactory FACTORY_OF_SELF = () -> INSTANCE;

	private EntityCopyNotAllowedObserver() {
		//Not to be constructed; use INSTANCE.
	}

	@Override
	public void entityCopyDetected(
			@Nonnull Object managedEntity,
			@Nonnull Object mergeEntity1,
			@Nonnull Object mergeEntity2,
			@Nonnull EventSource session) {
		if ( mergeEntity1 == managedEntity && mergeEntity2 == managedEntity) {
			throw new AssertionFailure( "entity1 and entity2 are the same as managedEntity; must be different" );
		}
		throw new IllegalStateException( "Multiple representations of the same entity "
				+ infoString( session.getEntityName( managedEntity ), session.getIdentifier( managedEntity ) )
				+ " are being merged: " + managedOrDetachedEntityString( managedEntity, mergeEntity1 )
				+ "; " + managedOrDetachedEntityString( managedEntity, mergeEntity2 ) );
	}

	private @Nonnull String managedOrDetachedEntityString(@Nonnull Object managedEntity, @Nonnull Object entity ) {
		return new StringBuilder()
				.append( entity == managedEntity ? "Managed" : "Detached" )
				.append( " [" )
				.append( entity )
				.append( ']' )
				.toString();
	}

	public void clear() {
		// Nothing to do
	}

	@Override
	public void topLevelMergeComplete(@Nonnull EventSource session) {
		// Nothing to do
	}
}
