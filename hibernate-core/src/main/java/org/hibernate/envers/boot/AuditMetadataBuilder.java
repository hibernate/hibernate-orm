/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot;

import org.hibernate.envers.configuration.internal.MappingCollector;

/**
 * Contract for specifying various overrides to be used in the AuditMetadata building.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public interface AuditMetadataBuilder {
	/**
	 * Specify whether entity names should be tracked within a revision.
	 *
	 * @param trackEntitiesChangedInRevision {@code true} to track entity names, {@code false} otherwise.
	 *
	 * @return {@code this}, for method chaining.
	 *
	 * @see org.hibernate.envers.configuration.EnversSettings#TRACK_ENTITIES_CHANGED_IN_REVISION
	 */
	AuditMetadataBuilder applyTrackEntitiesChangedInRevision(boolean trackEntitiesChangedInRevision);

	/**
	 * Specify the name of the entity to be used as the {@code @RevisionEntity}.
	 *
	 * @param revisionInfoEntityName the revision info entity name.
	 *
	 * @return {@code this}, for method chaining.
	 */
	AuditMetadataBuilder applyRevisionInfoEntityName(String revisionInfoEntityName);

	/**
	 * Specify the {@link MappingCollector} implementation to use.
	 *
	 * @param mappingCollector the mapping collector.
	 * @return {@code this}, for method chaining.
	 */
	AuditMetadataBuilder applyMappingCollector(MappingCollector mappingCollector);

	/**
	 * Buld the AuditMetadata.
	 *
	 * @return The built metadata.
	 */
	AuditMetadata build();
}
