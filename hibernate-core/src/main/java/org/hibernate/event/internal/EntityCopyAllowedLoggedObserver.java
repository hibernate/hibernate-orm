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
import org.hibernate.internal.util.collections.IdentitySet;

import static org.hibernate.event.internal.EntityCopyLogging.EVENT_COPY_LOGGER;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * An {@link EntityCopyObserver} implementation that allows multiple representations of
 * the same persistent entity to be merged and provides logging of the entity copies that
 * are detected.
 *
 * @author Gail Badner
 */
public final class EntityCopyAllowedLoggedObserver implements EntityCopyObserver {

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
		if ( EVENT_COPY_LOGGER.isTraceEnabled() ) {
			EVENT_COPY_LOGGER.duplicateRepresentationBeingMerged(
					infoString( entityName, session.getIdentifier( managedEntity ) )
			);
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
				EVENT_COPY_LOGGER.mergeSummaryMultipleRepresentations(
						entry.getKey(), entry.getValue() );
			}
		}
		else {
			EVENT_COPY_LOGGER.noEntityCopiesMerged();
		}

		if ( managedToMergeEntitiesXref != null ) {
			for ( var entry : managedToMergeEntitiesXref.entrySet() ) {
				final Object managedEntity = entry.getKey();
				EVENT_COPY_LOGGER.mergeDetails(
						entry.getValue().size(),
						infoString( session.getEntityName( managedEntity ),
								session.getIdentifier( managedEntity ) ),
						renderList( entry.getValue(), managedEntity ),
						String.valueOf( managedEntity )
				);
			}
		}
	}

	private String renderList(Set<Object> mergeEntities, Object managedEntity) {
		final var list = new StringBuilder();
		boolean first = true;
		for ( Object mergeEntity : mergeEntities ) {
			if ( first ) {
				first = false;
			}
			else {
				list.append(", ");
			}
			list.append( mergeEntity == managedEntity ? "Managed" : "Detached" )
					.append( " [" )
					.append( mergeEntity )
					.append( ']' );
		}
		return list.toString();
	}

}
