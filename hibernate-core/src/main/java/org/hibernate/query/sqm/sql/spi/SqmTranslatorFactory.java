/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.spi;

import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Global integration contract for creating single-use SQM-to-SQL-AST
/// translators.
///
/// This factory is not a Dialect extension point. Database-local structural
/// differences belong in focused Dialect capabilities. Framework integrations
/// may supply a factory through [org.hibernate.query.spi.QueryEngineOptions]
/// and must consume only the supported [SqmTranslationRequest] input.
///
/// A supplied factory is shared by the query engine and must be thread-safe.
/// It must return a fresh translator for every request and must not retain the
/// request after translation.
///
/// @author Steve Ebersole
/// @see org.hibernate.query.spi.QueryEngineOptions#getCustomSqmTranslatorFactory()
/// @see org.hibernate.query.spi.QueryEngine#getSqmTranslatorFactory()
@SPI({ IMPLEMENT, SUPPLY })
public interface SqmTranslatorFactory {
	/// Create a translator for one select request.
	SqmTranslator<SelectStatement> createSelectTranslator(
			SqmTranslationRequest.Select request);

	/// Create a translator for one mutation request.
	SqmTranslator<? extends MutationStatement> createMutationTranslator(
			SqmTranslationRequest.Mutation request);
}
