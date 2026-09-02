/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.query.sqm.sql.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.sql.ast.spi.Statement;

/**
 * The standard translator for SQM to SQL ASTs.
 *
 * @author Christian Beikov
 */
public class StandardSqmTranslator<T extends Statement> extends BaseSqmToSqlAstConverter<T> {

	public StandardSqmTranslator(SqmTranslationRequest<?> request) {
		super( request );
	}
}
