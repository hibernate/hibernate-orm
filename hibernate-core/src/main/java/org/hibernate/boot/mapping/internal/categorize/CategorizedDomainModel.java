/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.models.spi.ClassDetails;

/// The application's domain model after source collection has been interpreted and
/// categorized.
///
/// The categorized model exposes the persistent type structure needed by later
/// binding phases: entity hierarchies, mapped-superclasses, embeddables, persistent
/// attributes, and key mappings.  It also exposes persistence-unit scoped
/// {@linkplain #getGlobalRegistrations() global registrations} such as converters,
/// type registrations, filters, and generators.
///
/// Categorization uses bootstrap services such as the Hibernate Models class and
/// annotation descriptor registries, but those services are not part of this result.
/// Consumers that need categorization-time infrastructure should use
/// {@link CategorizationContext} instead.
///
/// @since 9.0
/// @author Steve Ebersole
public interface CategorizedDomainModel {
	/// Global registrations collected while processing the persistence-unit.
	GlobalRegistrations getGlobalRegistrations();

	/// All entity hierarchies defined in the persistence unit
	Set<EntityHierarchy> getEntityHierarchies();

	/// All source classes considered while categorizing the persistence unit.
	Map<String,ClassDetails> getSourceClasses();

	/// Iteration over the {@linkplain #getEntityHierarchies() entity hierarchies}
	default void forEachEntityHierarchy(IndexedConsumer<EntityHierarchy> hierarchyConsumer) {
		final Set<EntityHierarchy> entityHierarchies = getEntityHierarchies();
		if ( entityHierarchies.isEmpty() ) {
			return;
		}

		int pos = 0;
		for ( EntityHierarchy entityHierarchy : entityHierarchies ) {
			hierarchyConsumer.accept( pos, entityHierarchy );
			pos++;
		}
	}

	/// All mapped-superclasses defined in the persistence unit
	Map<String,ClassDetails> getMappedSuperclasses();

	/// Iteration over the {@linkplain #getMappedSuperclasses() mapped superclasses}
	default void forEachMappedSuperclass(KeyedConsumer<String, ClassDetails> consumer) {
		final Map<String, ClassDetails> mappedSuperclasses = getMappedSuperclasses();
		if ( mappedSuperclasses.isEmpty() ) {
			return;
		}

		mappedSuperclasses.forEach( consumer::accept );
	}

	/// All embeddables defined in the persistence unit
	Map<String,ClassDetails> getEmbeddables();

	/// Iteration over the {@linkplain #getEmbeddables() embeddables}
	default void forEachEmbeddable(KeyedConsumer<String, ClassDetails> consumer) {
		final Map<String, ClassDetails> embeddables = getEmbeddables();
		if ( embeddables.isEmpty() ) {
			return;
		}

		embeddables.forEach( consumer::accept );
	}
}
