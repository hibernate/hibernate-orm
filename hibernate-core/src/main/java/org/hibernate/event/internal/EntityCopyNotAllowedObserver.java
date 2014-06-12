/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.event.spi.EventSource;
import org.hibernate.pretty.MessageHelper;

/**
 * @author Gail Badner
 */
public class EntityCopyNotAllowedObserver implements EntityCopyObserver {

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
