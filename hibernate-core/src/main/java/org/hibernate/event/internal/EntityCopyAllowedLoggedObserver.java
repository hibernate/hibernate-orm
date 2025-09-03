/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;

import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * An {@link EntityCopyObserver} implementation that allows multiple representations of
 * the same persistent entity to be merged and provides logging of the entity copies that
 * are detected.
 *
 * @author Gail Badner
 */
public final class EntityCopyAllowedLoggedObserver implements EntityCopyObserver {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( EntityCopyAllowedLoggedObserver.class );

	public static final EntityCopyObserverFactory FACTORY_OF_SELF = EntityCopyAllowedLoggedObserver::new;

	public static final String SHORT_NAME = "log";

	// Tracks the number of entity copies per entity name.
	private Map<String, Integer> countsByEntityName;

	// managedToMergeEntitiesXref is only maintained for DEBUG logging so that a "nice" message
	// about multiple representations can be logged at the completion of the top-level merge.
	// If no entity copies have been detected, managedToMergeEntitiesXref will remain null;
	private Map<Object, Set<Object>> managedToMergeEntitiesXref = null;
		// key is the managed entity;
		// value is the set of representations being merged corresponding to the same managed result.

	private EntityCopyAllowedLoggedObserver() {
		//Not to be constructed directly; use FACTORY_OF_SELF.
	}

	@Override
	public void entityCopyDetected(
			Object managedEntity,
			Object mergeEntity1,
			Object mergeEntity2,
			EventSource session) {
		final String entityName = session.getEntityName( managedEntity );
		if ( log.isTraceEnabled() ) {
			log.trace( "More than one representation of the same persistent entity being merged for: "
						+ infoString( entityName, session.getIdentifier( managedEntity ) ) );
		}
		Set<Object> detachedEntitiesForManaged = null;
		if ( managedToMergeEntitiesXref == null ) {
			// This is the first time multiple representations have been found;
			// instantiate managedToMergeEntitiesXref.
			managedToMergeEntitiesXref = new IdentityHashMap<>();
		}
		else {
			// Get any existing representations that have already been found.
			detachedEntitiesForManaged = managedToMergeEntitiesXref.get( managedEntity );
		}
		if ( detachedEntitiesForManaged == null ) {
			// There were no existing representations for this particular managed entity;
			detachedEntitiesForManaged = new IdentitySet<>();
			managedToMergeEntitiesXref.put( managedEntity, detachedEntitiesForManaged );
			incrementEntityNameCount( entityName );
		}
		// Now add the detached representation for the managed entity;
		detachedEntitiesForManaged.add( mergeEntity1 );
		detachedEntitiesForManaged.add( mergeEntity2 );
	}

	private void incrementEntityNameCount(String entityName) {
		Integer countBeforeIncrement = 0;
		if ( countsByEntityName == null ) {
			// Use a TreeMap so counts can be logged by entity name in alphabetic order.
			countsByEntityName = new TreeMap<>();
		}
		else {
			countBeforeIncrement = countsByEntityName.get( entityName );
			if ( countBeforeIncrement == null ) {
				// no entity copies for entityName detected previously.
				countBeforeIncrement = 0;
			}
		}
		countsByEntityName.put( entityName, countBeforeIncrement + 1 );
	}

	public void clear() {
		if ( managedToMergeEntitiesXref != null ) {
			managedToMergeEntitiesXref.clear();
			managedToMergeEntitiesXref = null;
		}
		if ( countsByEntityName != null ) {
			countsByEntityName.clear();
			countsByEntityName = null;
		}
	}

	@Override
	public void topLevelMergeComplete(EventSource session) {
		// Log the summary.
		if ( countsByEntityName != null ) {
			for ( var entry : countsByEntityName.entrySet() ) {
				log.debug(
						String.format(
								"Summary: number of %s entities with multiple representations merged: %d",
								entry.getKey(),
								entry.getValue()
						)
				);
			}
		}
		else {
			log.debug( "No entity copies merged" );
		}

		if ( managedToMergeEntitiesXref != null ) {
			for ( var entry : managedToMergeEntitiesXref.entrySet() ) {
				final Object managedEntity = entry.getKey();
				final Set<Object> mergeEntities = entry.getValue();
				final var sb =
						new StringBuilder( "Details: merged ")
								.append( mergeEntities.size() )
								.append( " representations of the same entity " )
								.append( infoString( session.getEntityName( managedEntity ),
												session.getIdentifier( managedEntity ) ) )
								.append( " being merged: " );
				boolean first = true;
				for ( Object mergeEntity : mergeEntities ) {
					if ( first ) {
						first = false;
					}
					else {
						sb.append( ", " );
					}
					sb.append(  getManagedOrDetachedEntityString( managedEntity, mergeEntity ) );
				}
				sb.append( "; resulting managed entity: [" ).append( managedEntity ).append( ']' );
				log.debug( sb.toString());
			}
		}
	}

	private String getManagedOrDetachedEntityString(Object managedEntity, Object mergeEntity ) {
		return mergeEntity == managedEntity
				? "Managed: [" + mergeEntity + "]"
				: "Detached: [" + mergeEntity + "]";
	}
}
