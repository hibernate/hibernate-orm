/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

/// Read-only interpretation of an attribute declaration at one contextual
/// usage site.
///
/// Usage captures information which can vary without changing the source
/// declaration, most importantly generic type resolution and the source role
/// and path through which the declaration is reached.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeUsage {
	/// The persistent attribute name.
	String attributeName();

	/// The source declaration interpreted by this usage.
	AttributeDeclaration declaration();

	/// The source member of [#declaration()].
	MemberDetails member();

	/// The attribute type resolved in this usage's type-variable scope.
	TypeDetails resolvedType();

	/// A diagnostic description of the source container for this usage.
	String sourceRole();

	/// The attribute path relative to the source role.
	String attributePath();

	/// The categorized mapping nature at this usage site.
	AttributeNature nature();
}
