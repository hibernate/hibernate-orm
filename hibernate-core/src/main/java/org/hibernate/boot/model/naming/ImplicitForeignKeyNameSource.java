/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/// Naming dependencies for a foreign key, including its referenced table and columns.
/// The inherited table and columns describe the local, referencing side.
///
/// @author Steve Ebersole
public non-sealed interface ImplicitForeignKeyNameSource
		extends ImplicitConstraintNameSource {
	/// The referenced table identifier, using the same stage conventions as [#getTableName()].
	///
	/// @return The referenced table identifier
	Identifier getReferencedTableName();
	/// Referenced column identifiers recorded by the foreign-key mapping. This may be
	/// empty for a primary-key reference represented without explicit target columns.
	///
	/// @return The recorded referenced column identifiers
	List<Identifier> getReferencedColumnNames();

	@Override
	default Kind kind() {
		return Kind.FOREIGN_KEY;
	}
}
