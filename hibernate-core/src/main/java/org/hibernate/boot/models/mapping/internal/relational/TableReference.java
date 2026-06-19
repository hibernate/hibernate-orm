/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Table;

/// Binding-time reference to a relational table expression.
///
/// Following the SQL table-reference idea, a reference may represent a physical
/// table, secondary table, physical view, or inline view.  The reference carries the
/// logical name used by mapping sources and the Hibernate {@link Table} binding
/// created for later boot phases.
///
/// Known implementations include:
/// <ul>
///     <li>{@link PhysicalTable}</li>
///     <li>{@link SecondaryTable}</li>
///     <li>{@link PhysicalView}</li>
///     <li>{@link InLineView}</li>
/// </ul>
///
/// @since 9.0
/// @author Steve Ebersole
public interface TableReference {
	/// The name used across mapping sources such as annotations and XML.
	///
	/// For physical tables and views, the logical name might differ from the final
	/// physical name after the {@linkplain org.hibernate.boot.model.naming.PhysicalNamingStrategy}
	/// is applied.
	Identifier logicalName();

	/// Whether this table reference should be exposed to schema tooling.
	boolean exportable();

	/// The Hibernate mapping table object represented by this reference.
	Table binding();
}
