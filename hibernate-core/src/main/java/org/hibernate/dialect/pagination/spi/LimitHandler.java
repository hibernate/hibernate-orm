/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Dialect strategy for applying execution-time limit and offset to
/// already-rendered SQL.
///
/// This contract is used for pagination requested through
/// [org.hibernate.query.Query#setMaxResults] or
/// [org.hibernate.query.Query#setFirstResult]. It is distinct from
/// [org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport], which
/// selects syntax while translating pagination already represented in the SQL
/// AST.
///
/// Implementations should normally extend [AbstractLimitHandler] or one of its
/// supported syntax-family subclasses. [#processSql] must be side-effect free:
/// it returns rewritten SQL and explicit JDBC instructions instead of retaining
/// request-specific binding state in the handler. A Dialect may return the same
/// handler instance for its whole lifetime, so custom handlers must be safe for
/// concurrent use.
///
/// @see org.hibernate.dialect.Dialect#getLimitHandler()
/// @author Lukasz Antoniak
/// @since 8.0
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface LimitHandler {
	/**
	 * Does this handler support limiting query results?
	 *
	 * @return True if this handler supports limit alone.
	 */
	default boolean supportsLimit() {
		return false;
	}

	/**
	 * Does this handler support offsetting query results without
	 * also specifying a limit?
	 *
	 * @return True if this handler supports offset alone.
	 */
	default boolean supportsOffset() {
		return false;
	}

	/**
	 * Does this handler support combinations of limit and offset?
	 *
	 * @return True if the handler supports an offset within the limit support.
	 */
	default boolean supportsLimitOffset() {
		return supportsLimit();
	}

	/// Apply pagination to a complete SQL string.
	///
	/// Return [PaginationResult#unchanged] when the request needs no SQL or JDBC
	/// pagination. The returned JDBC instructions must describe every pagination
	/// parameter introduced by the rewritten SQL.
	///
	/// @param request immutable completed-SQL pagination input
	/// @return rewritten SQL and the JDBC work needed to execute it
	PaginationResult processSql(PaginationRequest request);

	/// Determine the one-based position of the first original query parameter
	/// after any pagination parameters inserted at the beginning of the SQL.
	///
	/// This method must agree with
	/// [PaginationJdbcInstructions#parametersAtStart] returned by [#processSql]
	/// and must not mutate this handler.
	default int parameterPositionStart(PaginationRequest request) {
		return 1;
	}

}
