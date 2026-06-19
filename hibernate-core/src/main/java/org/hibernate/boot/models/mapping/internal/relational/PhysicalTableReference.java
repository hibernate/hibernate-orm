/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.relational;

import org.hibernate.boot.model.naming.Identifier;

/// Persistent table reference with a concrete physical table name.
///
/// The physical name is the result of applying naming and quoting rules to the
/// logical mapping name.
///
/// @since 9.0
/// @author Steve Ebersole
public interface PhysicalTableReference extends PersistentTableReference {
	/// The physical table name used by the database model.
	Identifier getPhysicalTableName();
}
