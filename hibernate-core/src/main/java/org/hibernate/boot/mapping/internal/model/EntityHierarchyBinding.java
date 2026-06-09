/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.List;

import org.hibernate.engine.OptimisticLockStyle;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;
import jakarta.persistence.InheritanceType;

/// Binding-model node for one entity hierarchy.
///
/// An entity hierarchy is the natural owner for facts that are shared across
/// all entity types in the hierarchy: the root entity, any mapped-superclass
/// declarations above the root, inheritance strategy, default access strategy,
/// and optimistic-locking style.  Keeping this as a top-level binding node lets
/// later materialization consume hierarchy facts directly instead of
/// rediscovering them from individual entity bindings.
///
/// @since 9.0
/// @author Steve Ebersole
public record EntityHierarchyBinding(
		EntityTypeBinding root,
		ManagedTypeBinding absoluteRoot,
		List<Type> types,
		InheritanceType inheritanceType,
		AccessType defaultAccessType,
		OptimisticLockStyle optimisticLockStyle) {
	public EntityHierarchyBinding {
		types = List.copyOf( types );
	}

	/// One managed type's placement in the hierarchy.
	///
	/// The super type is nullable only for [Relation#SUPER] when the type is the
	/// absolute root above the entity root.
	public record Type(
			ManagedTypeBinding binding,
			@Nullable ManagedTypeBinding superType,
			Relation relation) {
	}

	/// Describes a managed type's position relative to the hierarchy root entity.
	public enum Relation {
		/// Mapped superclass before the root entity.
		SUPER,
		/// The hierarchy root entity.
		ROOT,
		/// Entity or mapped superclass below the root entity.
		SUB
	}
}
