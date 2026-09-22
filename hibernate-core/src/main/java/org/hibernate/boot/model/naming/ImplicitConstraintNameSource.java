/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import java.util.List;

/// Shared input for the remaining Identifier-based constraint and index callbacks.
/// These sources do not expose the paired logical/physical dependencies used by the
/// newer column and table inputs. Identifier values retain quoting but do not encode
/// their naming stage; callers supply the names available at constraint materialization.
///
/// @author Steve Ebersole
public sealed interface ImplicitConstraintNameSource
		extends ImplicitNameSource
		permits ImplicitIndexNameSource, ImplicitUniqueKeyNameSource {
	/// The table containing the constraint or index. Boot adapters use the registered
	/// logical table name when available and otherwise the mapping table name.
	///
	/// @return The table identifier
	Identifier getTableName();
	/// The participating local column identifiers in the order supplied by the binding.
	/// This contract does not provide selected logical/physical pairs or model SQL expressions.
	///
	/// @return The column identifiers used as naming dependencies
	List<Identifier> getColumnNames();
	/// The supplied constraint or index name, or null when no name was supplied.
	/// Normal boot binding bypasses implicit naming for a nonempty explicit name;
	/// this accessor remains part of the existing source contract.
	///
	/// @return The supplied identifier, or null
	Identifier getUserProvidedIdentifier();
	/// The constraint or index role represented by this source.
	///
	/// @return The naming role
	Kind kind();

	/// The database-object role being named.
	enum Kind {
		UNIQUE_KEY,
		INDEX
	}
}
