/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.models.spi.ClassDetails;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;

/// Immutable view of an entity processed before an [AdditionalMappingContributor] is invoked.
///
/// The three entity identifiers exposed here are intentionally distinct:
/// * the [Hibernate entity name][#getEntityName] identifies the boot-model entity binding
/// * the [JPA entity name][#getJpaEntityName] identifies the entity in queries
/// * the [class name][#getClassName] identifies its Java type when one exists.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface ProcessedEntity {
	/// The Hibernate entity name used to identify the entity binding.
	String getEntityName();

	/// The JPA entity name used to identify the entity in queries.
	String getJpaEntityName();

	/// The Java class name, or {@code null} for a dynamic entity.
	@Nullable
	String getClassName();

	/// The model representation of the entity class.
	ClassDetails getClassDetails();

	/// The access type determined while categorizing the entity.
	AccessType getAccessType();

	/// Whether this entity is the root entity of its inheritance hierarchy.
	boolean isHierarchyRoot();

	/// The nearest persistent super-entity's Hibernate entity name, if one.
	///
	/// A mapped-superclass between this entity and its persistent super-entity
	/// is skipped.
	@Nullable
	String getSuperEntityName();

	/// The names of the persistent attributes declared by this entity.
	Set<String> getDeclaredAttributeNames();
}
