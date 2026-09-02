/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The transition-image exposed by a data-change table expression.
///
/// @since 8.0
/// @author Steve Ebersole
public enum ChangeTableKind {
	/// The row image before the mutation.
	OLD,
	/// The row image immediately after the mutation, before trigger processing.
	NEW,
	/// The final row image after the mutation and trigger processing.
	FINAL
}
