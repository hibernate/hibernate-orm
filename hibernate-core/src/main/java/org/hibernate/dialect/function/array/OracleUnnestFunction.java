/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Oracle unnest function.
 */
public class OracleUnnestFunction extends UnnestFunction {

	public OracleUnnestFunction() {
		super( "column_value" );
	}

	@Override
	protected void renderUnnest(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "table(" );
		array.accept( walker );
		sqlAppender.appendSql( ")" );
	}
}
