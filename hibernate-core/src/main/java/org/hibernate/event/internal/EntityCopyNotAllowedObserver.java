/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventSource;
import org.hibernate.pretty.MessageHelper;

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
			Object managedEntity,
			Object mergeEntity1,
			Object mergeEntity2,
			EventSource session) {
		if ( mergeEntity1 == managedEntity && mergeEntity2 == managedEntity) {
			throw new AssertionFailure( "entity1 and entity2 are the same as managedEntity; must be different." );
		}
		final String managedEntityString = 	MessageHelper.infoString(
				session.getEntityName( managedEntity ),
				session.getIdentifier( managedEntity )
		);
		throw new IllegalStateException(
				"Multiple representations of the same entity " + managedEntityString + " are being merged. " +
						getManagedOrDetachedEntityString( managedEntity, mergeEntity1 ) + "; " +
						getManagedOrDetachedEntityString( managedEntity, mergeEntity2 )
		);
	}

	private String getManagedOrDetachedEntityString(Object managedEntity, Object entity ) {
		if ( entity == managedEntity) {
			return  "Managed: [" + entity + "]";
		}
		else {
			return "Detached: [" + entity + "]";
		}
	}

	public void clear() {
		// Nothing to do
	}

	@Override
	public void topLevelMergeComplete(EventSource session) {
		// Nothing to do
	}
}
