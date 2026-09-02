/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Factory supplied by a Dialect for creating single-use SQL AST translators.
///
/// Implementors should normally extend [StandardSqlAstTranslatorFactory] and
/// override its typed creation hook, returning a translator derived from
/// [AbstractSqlAstTranslator] or an appropriate supported family base. A
/// factory may inspect the [SqlAstTranslationRequest] subtype, but must preserve
/// its statement-to-JDBC-operation type relationship and must not retain the
/// request.
///
/// Hibernate may reuse a factory for the lifetime of the Dialect, so a supplied
/// factory should be stateless or otherwise safe for concurrent use. Each
/// translator returned by [#buildTranslator] is used for exactly one request.
///
/// @since 8.0
/// @author Steve Ebersole
/// @see org.hibernate.dialect.Dialect#getSqlAstTranslatorFactory()
@SPI({ IMPLEMENT, SUPPLY })
public interface SqlAstTranslatorFactory {
	/// Build a single-use translator for the given typed request.
	///
	/// @param request complete translation input; never `null`
	/// @return a non-null translator compatible with the request's result type
	<S extends Statement, O extends JdbcOperation>
	SqlAstTranslator<O> buildTranslator(SqlAstTranslationRequest<S, O> request);
}
