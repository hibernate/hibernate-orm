/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.models.spi.TypeDetails;
import org.hibernate.mapping.DeclarationRole;
import org.hibernate.mapping.MappingRole;

import static java.util.Objects.requireNonNull;

/// The semantic description of one attribute at one concrete place in the boot
/// mapping model.
///
/// This is the third part of the declaration/usage/application model:
///
/// 1. [AttributeDeclarationBinding] identifies the source member which declares
///    the attribute.
/// 2. [AttributeUsageBinding] interprets that declaration in a particular type
///    and path context, including generic type resolution.
/// 3. `AppliedAttributeMapping` gives that interpreted usage a stable
///    [MappingRole] in the concrete mapping graph.
///
/// For example, given the following model:
///
/// ```java
/// @MappedSuperclass
/// class Base<T> {
///     T value;
/// }
///
/// @Entity
/// class Customer extends Base<UUID> {
/// }
/// ```
///
/// there is one declaration of `Base.value`, a usage in which `T` resolves to
/// `UUID`, and an application whose role is equivalent to:
///
/// ```text
/// entity:Customer#attribute:value
/// ```
///
/// A second entity extending `Base<String>` refers to the same declaration but
/// has a different usage and application.  Direct entity attributes follow the
/// same model even when their declaration and usage facts happen to be
/// identical.
///
/// Applications are created for entity attributes, for concrete parameterized
/// embeddable usages retained at a mapped-superclass site, and for the direct
/// members of each applied embeddable.  They may describe basic, association,
/// embedded, or plural attributes.  Binding materializers use the role when
/// creating the corresponding `Property` and `Value` compatibility
/// projections.  Runtime/JPA metamodel handoff code then uses that role to find
/// the resolved usage without relying on the identity of a copied `Property`.
///
/// This record deliberately does not retain a `Property` or `Value`.  Those
/// mutable objects are compatibility projections of the semantic product and
/// may be copied, while [#role()] remains the intrinsic identity of the
/// application.
///
/// @since 9.0
/// @author Steve Ebersole
public record AppliedAttributeMapping(
		/// The source declaration interpreted for this concrete usage.
		AttributeUsageBinding usage,

		/// Stable identity of this attribute occurrence in the mapping graph.
		MappingRole role) {
	public AppliedAttributeMapping {
		requireNonNull( usage );
		requireNonNull( role );
		final MappingRole.Part localPart = role.getLocalPart();
		if ( localPart == null || localPart.kind() != MappingRole.PartKind.ATTRIBUTE ) {
			throw new IllegalArgumentException( "Applied attribute role must end in an attribute part: " + role );
		}
		if ( !localPart.name().equals( usage.attributeName() ) ) {
			throw new IllegalArgumentException(
					"Applied attribute role '" + role + "' does not match usage '" + usage.attributeName() + "'"
			);
		}
	}

	/// The source declaration realized by this application.
	///
	/// Multiple applications may return the same declaration.  In the example
	/// above, both `Customer.value` and another entity's `value` application
	/// refer to the declaration of `Base.value`.
	public AttributeDeclarationBinding declaration() {
		return usage.declaration();
	}

	/// Stable identity of the source declaration, as opposed to the identity of
	/// this concrete occurrence returned by [#role()].
	public DeclarationRole declarationRole() {
		return declaration().declarationRole();
	}

	/// The concrete mapping container to which the attribute is applied.
	///
	/// For `entity:Customer#attribute:address.city`, for example, this is the
	/// role of the `address` component.
	public MappingRole containerRole() {
		return role.getParent();
	}

	/// The member type resolved for this concrete usage, including any generic
	/// specialization introduced by the application context.
	public TypeDetails resolvedType() {
		return usage.resolvedType();
	}
}
