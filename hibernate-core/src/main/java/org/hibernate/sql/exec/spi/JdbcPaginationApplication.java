/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.SPI;

/// Records which stage owns pagination for a JDBC select plan.
///
/// Exactly one stage must own an effective limit or offset. Execution
/// finalization uses this value to avoid applying a
/// [org.hibernate.dialect.pagination.spi.LimitHandler] to a plan whose
/// translator or JDBC metadata already represents pagination.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public enum JdbcPaginationApplication {
	/// The plan has no effective limit or offset.
	NONE,
	/// Pagination syntax and parameters were rendered by the SQL AST translator.
	RENDERED,
	/// Pagination is represented entirely by JDBC maximum-row or result-set
	/// skipping instructions.
	JDBC,
	/// Pagination must still be applied to the completed SQL by a
	/// [org.hibernate.dialect.pagination.spi.LimitHandler].
	RAW_SQL
}
