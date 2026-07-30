/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.AccessType;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// Read-only categorized usage of an embeddable declaration.
///
/// The same [EmbeddableTypeMetadata] may have multiple usages with different
/// source members, generic type-variable scopes, effective access strategies,
/// or categorized attributes.
///
/// @since 9.0
/// @author Steve Ebersole
public interface EmbeddableUsageMetadata {
	/// The usage-independent embeddable declaration.
	EmbeddableTypeMetadata type();

	/// The member through which the embeddable is used, or `null` for a usage
	/// without a source member.
	@Nullable
	MemberDetails sourceMember();

	/// The scope used to resolve generic types for this usage.
	TypeVariableScope typeVariableScope();

	/// The effective access strategy for this usage.
	AccessType accessType();

	/// The attributes categorized in this usage context.
	List<? extends AttributeMetadata> attributes();

	/// Finds a categorized attribute by name, returning `null` when none exists.
	default AttributeMetadata findAttribute(String name) {
		for ( AttributeMetadata attribute : attributes() ) {
			if ( attribute.getName().equals( name ) ) {
				return attribute;
			}
		}
		return null;
	}
}
