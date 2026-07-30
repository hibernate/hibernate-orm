/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import jakarta.persistence.AccessType;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;

/// Read-only semantic description of the source declaration of a persistent
/// attribute.
///
/// A declaration is independent of any inherited, generic, or embeddable usage
/// of the attribute. Use [AttributeUsage] for a contextual interpretation and
/// [AttributeApplication] for a concrete occurrence in the boot mapping graph.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeDeclaration {
	/// The stable identity of this declaration.
	DeclarationRole declarationRole();

	/// The persistent attribute name.
	String attributeName();

	/// The source member declaring the attribute.
	MemberDetails member();

	/// The access strategy used to discover and access the declaration.
	AccessType accessType();

	/// The categorized mapping nature of the declaration.
	AttributeNature nature();
}
