/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.spi;

import org.hibernate.SPI;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Standard [SqmTranslatorFactory] and reusable base for global integrations.
///
/// Subclasses may override either creation method but must preserve the
/// factory's single-use translator lifecycle and the request's statement type.
/// Database-specific query structure belongs in focused Dialect strategies.
///
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public class StandardSqmTranslatorFactory implements SqmTranslatorFactory {
	/// Create the standard stateless factory.
	@SPI(IMPLEMENT)
	public StandardSqmTranslatorFactory() {
	}

	@Override
	public SqmTranslator<SelectStatement> createSelectTranslator(
			SqmTranslationRequest.Select request) {
		return new StandardSqmTranslator<>( request );
	}

	@Override
	public SqmTranslator<? extends MutationStatement> createMutationTranslator(
			SqmTranslationRequest.Mutation request) {
		return new StandardSqmTranslator<>( request );
	}
}
