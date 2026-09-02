/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The semantic kind of table join being rendered.
///
/// @since 8.0
/// @author Steve Ebersole
public enum TableJoinKind {
	/// A join between semantic table groups.
	TABLE_GROUP,
	/// A join between physical table references inside one table group.
	TABLE_REFERENCE
}
