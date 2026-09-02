/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.query.sqm.sql.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.sql.ast.spi.Statement;

/// Provider-defined SQM translator used to verify the supported extension
/// boundary exposed by [SqmTranslationRequest] and
/// [BaseSqmToSqlAstConverter].
///
/// @param <T> the produced SQL AST statement type
///
/// @author Steve Ebersole
public final class ExampleSqmTranslator<T extends Statement> extends BaseSqmToSqlAstConverter<T> {
	public ExampleSqmTranslator(SqmTranslationRequest<?> request) {
		super( request );
	}
}
