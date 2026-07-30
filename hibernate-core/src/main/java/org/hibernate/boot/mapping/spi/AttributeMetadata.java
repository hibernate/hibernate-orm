/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// Read-only categorized description of a persistent attribute discovered on a
/// managed type.
///
/// This contract describes the categorized member and its declared type. For
/// the mapped value shape, use [SingularAttributeMetadata#getValue()] or the
/// element, index, and identifier values of [PluralAttributeMetadata].
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeMetadata {
	/// The persistent attribute name.
	String getName();

	/// The categorized mapping nature.
	AttributeNature getNature();

	/// The attribute's declared type.
	TypeDetails getAttributeType();

	/// Resolves the member type relative to a contextual type-variable scope.
	default TypeDetails resolveAttributeType(TypeVariableScope usageSite) {
		return getMember().resolveRelativeType( usageSite );
	}

	/// The persistent Java member.
	MemberDetails getMember();
}
