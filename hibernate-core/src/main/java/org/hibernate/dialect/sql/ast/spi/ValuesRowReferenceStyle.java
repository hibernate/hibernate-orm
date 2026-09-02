/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// How an on-duplicate-key clause refers to the proposed insert row.
///
/// @since 8.0
/// @author Steve Ebersole
public enum ValuesRowReferenceStyle {
	/// Use the legacy `values(column)` function.
	VALUES_FUNCTION,

	/// Declare and use an explicit alias for the proposed row.
	ROW_ALIAS,

	/// Use the database's implicit `excluded` pseudo-row.
	IMPLICIT_EXCLUDED
}
