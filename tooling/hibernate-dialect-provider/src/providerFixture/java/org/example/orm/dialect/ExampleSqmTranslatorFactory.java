/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.query.sqm.sql.spi.SqmTranslator;
import org.hibernate.query.sqm.sql.spi.SqmTranslatorFactory;
import org.hibernate.sql.ast.spi.query.MutationStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;

/// Provider implementation of the global SQM translator integration contract.
///
/// This factory deliberately consumes only supported request contracts and
/// does not depend on Hibernate's internal parameter-correlation model.
///
/// @author Steve Ebersole
public final class ExampleSqmTranslatorFactory implements SqmTranslatorFactory {
	@Override
	public SqmTranslator<SelectStatement> createSelectTranslator(SqmTranslationRequest.Select request) {
		return new ExampleSqmTranslator<>( request );
	}

	@Override
	public SqmTranslator<? extends MutationStatement> createMutationTranslator(SqmTranslationRequest.Mutation request) {
		return new ExampleSqmTranslator<>( request );
	}
}
