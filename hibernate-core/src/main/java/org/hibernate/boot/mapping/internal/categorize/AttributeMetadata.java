/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;


import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.relational.TableOwner;
import org.hibernate.models.spi.MemberDetails;

/// Categorized metadata about a persistent attribute.
///
/// Attribute metadata points back to the member selected for persistence and
/// exposes the broad mapping nature determined during categorization.  Detailed
/// value binding is handled later by the binding phase.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeMetadata extends TableOwner {
	/// The attribute name
	String getName();

	/// The persistent nature of the attribute
	AttributeNature getNature();

	/// The backing member
	MemberDetails getMember();
}
