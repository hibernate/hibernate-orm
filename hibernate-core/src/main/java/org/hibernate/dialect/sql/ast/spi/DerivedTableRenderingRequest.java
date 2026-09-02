/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.List;

/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// a derived-table reference.
///
/// Implementations of [DerivedTableRenderingSupport] should not mutate objects
/// reachable through this request or retain the request after plan selection.
///
/// @since 8.0
/// @author Steve Ebersole
public interface DerivedTableRenderingRequest {
	/// The kind of derived table being rendered.
	DerivedTableKind kind();

	/// Whether the reference is semantically lateral.
	boolean lateral();

	/// Whether the referenced query part is its statement's root query part.
	boolean queryPartRoot();

	/// The immutable list of derived-column names requested by the SQL AST.
	List<String> columnNames();

	/// Whether the database accepts an explicit `lateral` keyword in this
	/// context.
	boolean supportsLateralKeyword();
}
