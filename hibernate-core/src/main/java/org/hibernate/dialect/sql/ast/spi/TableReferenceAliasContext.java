/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The semantic location where a table-reference alias is rendered.
///
/// @since 8.0
/// @author Steve Ebersole
public enum TableReferenceAliasContext {
	/// Alias of a table reference in an ordinary from clause.
	FROM,
	/// Alias of an insert target table.
	INSERT_TARGET,
	/// Alias of an update target table.
	UPDATE_TARGET,
	/// Alias of a delete target table.
	DELETE_TARGET
}
