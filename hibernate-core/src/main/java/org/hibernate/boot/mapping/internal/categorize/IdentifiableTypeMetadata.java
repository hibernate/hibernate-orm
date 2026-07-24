/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.boot.mapping.internal.relational.TableOwner;

/// Categorized metadata about an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}.
///
/// Identifiable types are entities and mapped-superclasses that participate in an
/// entity hierarchy.  The hierarchy links exposed here are limited to the visible
/// types considered during categorization.
///
/// @since 9.0
/// @author Steve Ebersole
public interface IdentifiableTypeMetadata extends ManagedTypeMetadata, TableOwner {
	/// The hierarchy in which this IdentifiableType occurs.
	EntityHierarchy getHierarchy();

	/// The super-type, if one
	IdentifiableTypeMetadata getSuperType();

	/// Whether this type is considered abstract.
	default boolean isAbstract() {
		return getClassDetails().isAbstract();
	}

	/// Whether this type has subtypes
	boolean hasSubTypes();

	/// Get the number of direct subtypes
	int getNumberOfSubTypes();

	/// Get the direct subtypes
	Iterable<IdentifiableTypeMetadata> getSubTypes();

	/// Visit each direct subtype
	void forEachSubType(Consumer<IdentifiableTypeMetadata> consumer);

	/// Event listeners in effect for this type, minus
	/// {@linkplain jakarta.persistence.ExcludeDefaultListeners default listeners}.
	///
	/// @apiNote Kept separate from {@linkplain #getCompleteJpaEventListeners()}
	/// to facilitate types building their complete set with their
	/// {@linkplain jakarta.persistence.ExcludeSuperclassListeners superclass listeners}.
	List<JpaEventListener> getHierarchyJpaEventListeners();

	/// Event listeners in effect for this type, including
	/// {@linkplain jakarta.persistence.ExcludeDefaultListeners default listeners}
	List<JpaEventListener> getCompleteJpaEventListeners();
}
