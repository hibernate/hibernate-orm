/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.model;

import jakarta.persistence.AccessType;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;

/// Binding-model contract for the source-side declaration of an attribute.
///
/// A declaration binding describes where an attribute originates: declaration
/// container, source member, effective declaration access where one is stable,
/// and broad attribute nature.  It does not by itself imply a concrete mapped
/// usage; generic specialization, mapped-superclass contribution, embedded
/// paths, collection contexts, and usage-site access decisions may each require
/// separate [AttributeUsageBinding] nodes.
///
/// There are intentionally different declaration implementations for
/// identifiable managed types and embeddables:
///
/// - [IdentifiableAttributeDeclarationBinding] is stable for entity and
///   mapped-superclass declarations.
/// - [EmbeddableAttributeDeclarationBinding] records an embeddable-local source
///   member but leaves usage-site-sensitive interpretation to the usage binding.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeDeclarationBinding {
	/// The persistent attribute name as declared by the source member.
	///
	/// This is the local declaration name, not a dotted usage path.  Usage
	/// bindings may place this declaration at a nested path.
	String attributeName();

	/// The managed type that owns this declaration.
	///
	/// For identifiable declarations this is the entity or mapped superclass
	/// that declares the persistent member.  For embeddable declarations this is
	/// the embeddable type that contributes the member candidate; usage bindings
	/// still own usage-site-sensitive interpretation.
	ManagedTypeBinding declarationContainer();

	/// Alias for [#declarationContainer()] retained while callers migrate to the
	/// declaration/usage vocabulary.
	default ManagedTypeBinding declaringType() {
		return declarationContainer();
	}

	/// The source member associated with this declaration.
	///
	/// For [EmbeddableAttributeDeclarationBinding], the concrete usage may still
	/// refine effective access/member interpretation when the embeddable does not
	/// declare explicit access.
	MemberDetails member();

	/// The declaration-side access strategy known for this source member.
	///
	/// Identifiable declarations treat this as stable.  Embeddable declarations
	/// expose the best declaration-side access known at discovery time; the
	/// corresponding usage binding remains the authority for usage-site access.
	AccessType accessType();

	/// Broad persistent attribute kind for this declaration.
	///
	/// A usage may expose the same kind or a more usage-specific interpretation
	/// as the binding model grows.
	AttributeNature nature();
}
