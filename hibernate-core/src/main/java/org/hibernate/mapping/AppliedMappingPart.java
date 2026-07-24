/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Internal;

/// A compatibility mapping projection which represents a concrete applied
/// mapping occurrence.
///
/// The role identifies the occurrence independently of Java object identity.
/// A `null` role indicates a declaration-side projection or a mapping which has
/// not yet been applied to a concrete mapping container.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public interface AppliedMappingPart {
	/// The stable identity of this concrete occurrence, or `null` for a
	/// declaration-side or not-yet-applied projection.
	MappingRole getMappingRole();

	/// Assigns the stable identity of this concrete occurrence.
	void setMappingRole(MappingRole mappingRole);
}
