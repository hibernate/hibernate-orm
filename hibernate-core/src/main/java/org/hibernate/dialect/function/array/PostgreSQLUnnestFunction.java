/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;


/**
 * PostgreSQL unnest function.
 */
public class PostgreSQLUnnestFunction extends UnnestFunction {

	public PostgreSQLUnnestFunction() {
		super( null, "ordinality" );
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final AggregateSupport aggregateSupport = walker.getSessionFactory().getJdbcServices().getDialect()
				.getAggregateSupport();
		sqlAppender.appendSql( "(select" );
		tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
			if ( selectionIndex == 0 ) {
				sqlAppender.append( ' ' );
			}
			else {
				sqlAppender.append( ',' );
			}
			if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
				sqlAppender.appendSql( "t.ordinality" );
			}
			else {
				sqlAppender.append( aggregateSupport.aggregateComponentCustomReadExpression(
						"",
						"",
						"t.value",
						selectableMapping.getSelectableName(),
						SqlTypes.JSON,
						selectableMapping
				) );
			}
			sqlAppender.append( ' ' );
			sqlAppender.append( selectableMapping.getSelectionExpression() );
		} );
		sqlAppender.appendSql( " from jsonb_array_elements(" );
		array.accept( walker );
		sqlAppender.appendSql( ')' );
		if ( tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null ) != null ) {
			sqlAppender.appendSql( " with ordinality" );
		}
		sqlAppender.appendSql( " t)" );
	}
}
