/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import jakarta.persistence.AccessType;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;

/// Declaration binding for an attribute member declared by an embeddable type.
///
/// Embeddable attribute declarations are intentionally weaker than
/// [IdentifiableAttributeDeclarationBinding] declarations.  For entities and
/// mapped superclasses, the declaring managed type hierarchy determines the
/// effective access strategy and selected persistent member.  For embeddables,
/// the concrete embedded usage site may participate in that decision unless the
/// embeddable type declares its own explicit `@Access`.
///
/// In other words, this declaration records the source member candidate and the
/// embeddable type that declares it, but it is not the final semantic owner of
/// usage-sensitive facts such as:
///
/// - the effective access strategy at a particular embedded site;
/// - the resolved member type after generic specialization;
/// - override and conversion lookup paths;
/// - implicit naming inputs;
/// - table/storage context;
/// - compatibility objects produced by materialization.
///
/// Those facts belong to the corresponding [AttributeUsageBinding], typically a
/// [ComponentMemberBinding].  Keeping this distinction explicit lets the binding
/// model keep `AttributeUsageBinding#declaration()` non-null without pretending
/// that every embeddable member has one globally stable declaration independent
/// of where the embeddable is used.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddableAttributeDeclarationBinding(
		String attributeName,
		ManagedTypeBinding declarationContainer,
		MemberDetails member,
		AccessType accessType,
		AttributeNature nature) implements AttributeDeclarationBinding {
}
