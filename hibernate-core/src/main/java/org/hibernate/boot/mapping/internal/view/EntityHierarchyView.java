/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.view;

import java.util.List;

import org.hibernate.boot.mapping.internal.model.EntityHierarchyBinding;
import org.hibernate.boot.mapping.internal.model.EntityTypeBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.engine.OptimisticLockStyle;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;
import jakarta.persistence.InheritanceType;

/// Stable read view over an entity hierarchy binding.
///
/// The view exposes hierarchy-level boot facts without exposing the eventual
/// legacy `RootClass`/`Subclass` mapping objects as semantic state.
///
/// @since 9.0
/// @author Steve Ebersole
public record EntityHierarchyView(EntityHierarchyBinding binding) {
	public EntityTypeBinding root() {
		return binding.root();
	}

	public ManagedTypeBinding absoluteRoot() {
		return binding.absoluteRoot();
	}

	public List<EntityHierarchyBinding.Type> types() {
		return binding.types();
	}

	public List<EntityTypeBinding> entityTypes() {
		return binding.types()
				.stream()
				.map( EntityHierarchyBinding.Type::binding )
				.filter( EntityTypeBinding.class::isInstance )
				.map( EntityTypeBinding.class::cast )
				.toList();
	}

	public List<ManagedTypeBinding> mappedSuperclassTypes() {
		return binding.types()
				.stream()
				.map( EntityHierarchyBinding.Type::binding )
				.filter( (type) -> type.kind() == ManagedTypeBinding.Kind.MAPPED_SUPERCLASS )
				.toList();
	}

	public InheritanceType inheritanceType() {
		return binding.inheritanceType();
	}

	public AccessType defaultAccessType() {
		return binding.defaultAccessType();
	}

	public OptimisticLockStyle optimisticLockStyle() {
		return binding.optimisticLockStyle();
	}

	public @Nullable EntityHierarchyBinding.Type rootType() {
		for ( EntityHierarchyBinding.Type type : binding.types() ) {
			if ( type.relation() == EntityHierarchyBinding.Relation.ROOT ) {
				return type;
			}
		}
		return null;
	}
}
