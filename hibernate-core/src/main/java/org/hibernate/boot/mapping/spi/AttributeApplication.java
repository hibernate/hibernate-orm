/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import org.hibernate.models.spi.TypeDetails;

/// Read-only semantic description of one concrete attribute occurrence in the
/// boot mapping graph.
///
/// An application correlates a contextual [AttributeUsage] with the stable
/// [MappingRole] assigned to its materialized `org.hibernate.mapping`
/// occurrence. Multiple applications may therefore refer to the same
/// declaration or usage while having distinct mapping roles.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeApplication {
	/// The contextual usage materialized by this application.
	AttributeUsage usage();

	/// The stable identity of the concrete boot mapping occurrence.
	MappingRole role();

	/// The source declaration, obtained from [#usage()].
	default AttributeDeclaration declaration() {
		return usage().declaration();
	}

	/// The stable declaration identity.
	default DeclarationRole declarationRole() {
		return declaration().declarationRole();
	}

	/// The role of the mapping container, or `null` when [#role()] identifies a
	/// root.
	default MappingRole containerRole() {
		return role().getParent();
	}

	/// The Java type resolved for this application.
	default TypeDetails resolvedType() {
		return usage().resolvedType();
	}
}
