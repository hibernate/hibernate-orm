/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.models.categorize.CategorizationLogging;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.models.spi.ClassDetails;

/// Tracks mapped superclasses that are available but not used by any built entity hierarchy.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappedSuperclassTracker {
	private final Map<String, ClassDetails> unusedMappedSuperclasses;

	MappedSuperclassTracker(ManagedTypeInheritanceState inheritanceState) {
		if ( inheritanceState != null && CategorizationLogging.CATEGORIZATION_LOGGER.isDebugEnabled() ) {
			unusedMappedSuperclasses = new HashMap<>();
			inheritanceState.getMappedSuperclasses().forEach( (mappedSuperclass) -> {
				if ( mappedSuperclass.getClassName() != null ) {
					unusedMappedSuperclasses.put( mappedSuperclass.getClassName(), mappedSuperclass );
				}
			} );
		}
		else {
			unusedMappedSuperclasses = null;
		}
	}

	void markVisited(MappedSuperclassTypeMetadata mappedSuperclass) {
		if ( unusedMappedSuperclasses != null ) {
			unusedMappedSuperclasses.remove( mappedSuperclass.getClassDetails().getClassName() );
		}
	}

	void warnAboutUnusedMappedSuperclasses() {
		if ( unusedMappedSuperclasses == null ) {
			return;
		}

		for ( Map.Entry<String, ClassDetails> entry : unusedMappedSuperclasses.entrySet() ) {
			CategorizationLogging.CATEGORIZATION_LOGGER.debugf(
					"Encountered MappedSuperclass [%s] which was unused in any entity hierarchies",
					entry.getKey()
			);
		}
	}
}
