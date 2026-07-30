/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.models.spi.ClassDetails;

/// Root read-only view of the domain model produced by categorization.
///
/// This model describes mapping-source semantics. It does not expose mutable
/// boot mappings, categorization services, or unresolved working state.
///
/// @since 9.0
/// @author Steve Ebersole
public interface CategorizedDomainModel {
	/// The categorized entity hierarchies.
	Set<? extends EntityHierarchy> getEntityHierarchies();

	/// All source classes known to categorization, keyed by class name.
	Map<String, ClassDetails> getSourceClasses();

	/// Mapped-superclass source classes, keyed by class name.
	Map<String, ClassDetails> getMappedSuperclasses();

	/// Embeddable declarations, keyed by class name.
	Map<String, ? extends EmbeddableTypeMetadata> getEmbeddables();
}
