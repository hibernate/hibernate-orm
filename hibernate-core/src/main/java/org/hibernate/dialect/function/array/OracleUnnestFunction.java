/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
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
		super( "column_value", "i" );
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
		final ModelPart ordinalitySubPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		final boolean withOrdinality = ordinalitySubPart != null;
		if ( withOrdinality ) {
			sqlAppender.appendSql( "lateral (select t.*, rownum " );
			sqlAppender.appendSql( ordinalitySubPart.asBasicValuedModelPart().getSelectionExpression() );
			sqlAppender.appendSql( " from " );
		}
		sqlAppender.appendSql( "table(" );
		array.accept( walker );
		sqlAppender.appendSql( ")" );
		if ( withOrdinality ) {
			sqlAppender.appendSql( " t)" );
		}
	}
}
