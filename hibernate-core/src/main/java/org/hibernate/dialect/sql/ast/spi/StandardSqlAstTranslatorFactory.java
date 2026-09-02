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
import static org.hibernate.SPI.Role.USE;

/// Standard [SqlAstTranslatorFactory] and supported base for custom factories.
///
/// The public entry point preserves request validation and lifecycle. Subclasses
/// customize only [#createTranslator] and should return a fresh, single-use
/// translator for every invocation.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public class StandardSqlAstTranslatorFactory implements SqlAstTranslatorFactory {

	@Override
	public final <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> buildTranslator(
			SqlAstTranslationRequest<S, O> request) {
		return createTranslator( request );
	}

	/// Create the translator for a validated typed request.
	///
	/// Subclasses should normally construct an [AbstractSqlAstTranslator] based
	/// implementation. They must not cache or reuse the returned translator.
	///
	/// @return a fresh translator compatible with the request's result type
	protected <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> createTranslator(
			SqlAstTranslationRequest<S, O> request) {
		return new StandardSqlAstTranslator<>( request );
	}
}
