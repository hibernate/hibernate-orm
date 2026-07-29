/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;


import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.relational.TableOwner;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// Categorized metadata about a persistent attribute.
///
/// Attribute metadata points back to the member selected for persistence and
/// exposes the broad mapping nature determined during categorization.
/// [SingularAttributeMetadata] separates that member/ownership information
/// from its [ValueMetadata], while plural attributes expose their element and
/// index values separately. Detailed relational value binding is handled later
/// by the binding phase.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeMetadata extends TableOwner {
	/// The attribute name
	String getName();

	/// The persistent nature of the attribute
	AttributeNature getNature();

	/// The Java type of the attribute itself at its categorized declaration site.
	///
	/// For a singular attribute this is its value type. For a plural attribute
	/// this is the collection-container type, not its element or index type.
	TypeDetails getAttributeType();

	/// Resolves the attribute type for a particular applied usage, such as a
	/// concrete entity applying a generic mapped-superclass declaration.
	default TypeDetails resolveAttributeType(TypeVariableScope usageSite) {
		return getMember().resolveRelativeType( usageSite );
	}

	/// The backing member
	MemberDetails getMember();
}
