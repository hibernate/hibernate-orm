/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard json_objectagg function that uses no returning clause.
 */
public class H2JsonObjectAggFunction extends JsonObjectAggFunction {

	public H2JsonObjectAggFunction(TypeConfiguration typeConfiguration) {
		super( ":", true, typeConfiguration );
	}

	@Override
	protected void renderReturningClause(
			SqlAppender sqlAppender,
			JsonObjectAggArguments arguments,
			SqlAstTranslator<?> translator) {
		// No-op
	}
}
