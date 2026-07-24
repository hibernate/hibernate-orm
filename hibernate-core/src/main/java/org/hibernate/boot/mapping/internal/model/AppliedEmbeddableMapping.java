/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.HashSet;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.mapping.MappingRole;

import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/// The semantic description of one embeddable at one concrete place in the
/// boot mapping model.
///
/// An [EmbeddableContribution] describes the source facts contributed by an
/// embeddable type at a usage path.  `AppliedEmbeddableMapping` assigns those
/// facts a stable [MappingRole] and records the direct
/// [AppliedAttributeMapping]s created at that place.
///
/// This distinction matters whenever the same embeddable declaration is used
/// more than once:
///
/// ```java
/// @Embeddable
/// class Address {
///     String city;
/// }
///
/// @Entity
/// class Customer {
///     @Embedded Address homeAddress;
///     @Embedded Address workAddress;
/// }
/// ```
///
/// The two applications share the `Address` declaration, but have distinct
/// component and member roles:
///
/// ```text
/// entity:Customer#attribute:homeAddress
/// entity:Customer#attribute:homeAddress.city
///
/// entity:Customer#attribute:workAddress
/// entity:Customer#attribute:workAddress.city
/// ```
///
/// Separate applications are likewise needed for embedded identifiers,
/// embeddable collection elements, map keys, nested embeddables, aggregate
/// mappings, and embeddables whose generic members resolve differently at
/// different usage sites.
///
/// [#attributes()] contains only the direct persistent members of this
/// application.  A nested embeddable has its own `AppliedEmbeddableMapping`
/// under the role of its containing attribute.  A parent-reference member is
/// not a persistent applied attribute and is therefore not included.
///
/// Binding materializers create the mutable `Component` and `Property`
/// compatibility projections from these semantic products.  Runtime/JPA
/// metamodel handoff code resolves a component's role back to this record to
/// recover the correct source member and resolved type.  This record therefore
/// does not retain a `Component`; correlating by role remains valid when legacy
/// mapping objects are copied.
///
/// @since 9.0
/// @author Steve Ebersole
public record AppliedEmbeddableMapping(
		/// The source and path-sensitive facts realized at this application.
		EmbeddableContribution contribution,

		/// Stable identity of this component occurrence in the mapping graph.
		MappingRole role,

		/// The direct persistent attribute applications within this component.
		List<AppliedAttributeMapping> attributes) {
	public AppliedEmbeddableMapping {
		requireNonNull( contribution );
		requireNonNull( role );
		attributes = List.copyOf( attributes );
		if ( role.getLocalPart() == null ) {
			throw new IllegalArgumentException( "Applied embeddable role must identify a mapping part: " + role );
		}
		final HashSet<MappingRole> attributeRoles = new HashSet<>();
		for ( AppliedAttributeMapping attribute : attributes ) {
			if ( !role.equals( attribute.containerRole() ) ) {
				throw new IllegalArgumentException(
						"Applied attribute '" + attribute.role()
								+ "' does not belong to embeddable '" + role + "'"
				);
			}
			if ( !attributeRoles.add( attribute.role() ) ) {
				throw new IllegalArgumentException(
						"Duplicate applied attribute role within embeddable '" + role + "': " + attribute.role()
				);
			}
		}
	}

	/// The Java model type contributed at this application site.
	///
	/// This is the embeddable declaration type, not necessarily the resolved
	/// type of each of its generically-specialized members.
	public ClassDetails componentType() {
		return contribution.componentType();
	}

	/// Find a directly applied attribute by its local name.
	///
	/// This intentionally does not search nested embeddables.  Resolve the
	/// nested component's role to its own `AppliedEmbeddableMapping` instead.
	public @Nullable AppliedAttributeMapping findAttribute(String attributeName) {
		for ( AppliedAttributeMapping attribute : attributes ) {
			if ( attribute.usage().attributeName().equals( attributeName ) ) {
				return attribute;
			}
		}
		return null;
	}

	/// Find a directly applied attribute by its concrete mapping role.
	///
	/// Returns `null` when the role belongs to another component or names a
	/// nested descendant instead of a direct member.
	public @Nullable AppliedAttributeMapping findAttribute(MappingRole attributeRole) {
		if ( !role.equals( attributeRole.getParent() ) ) {
			return null;
		}
		for ( AppliedAttributeMapping attribute : attributes ) {
			if ( attribute.role().equals( attributeRole ) ) {
				return attribute;
			}
		}
		return null;
	}
}
