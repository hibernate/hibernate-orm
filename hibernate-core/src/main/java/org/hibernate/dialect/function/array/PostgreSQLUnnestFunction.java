/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;


/**
 * PostgreSQL unnest function.
 */
public class PostgreSQLUnnestFunction extends UnnestFunction {

	private final boolean supportsJsonTable;

	public PostgreSQLUnnestFunction(boolean supportsJsonTable) {
		super( null, "ordinality" );
		this.supportsJsonTable = supportsJsonTable;
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
		if ( supportsJsonTable ) {
			super.renderJsonTable(
					sqlAppender,
					array,
					pluralType,
					sqlTypedMapping,
					tupleType,
					tableIdentifierVariable,
					walker
			);
		}
		else {
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
					sqlAppender.appendSql( "t.i" );
				}
				else {
					sqlAppender.append( aggregateSupport.aggregateComponentCustomReadExpression(
							"",
							"",
							"t.v",
							selectableMapping.getSelectableName(),
							SqlTypes.JSON,
							selectableMapping,
							walker.getSessionFactory().getTypeConfiguration()
					) );
				}
				sqlAppender.append( " as " );
				sqlAppender.append( selectableMapping.getSelectionExpression() );
			} );
			sqlAppender.appendSql( " from jsonb_array_elements(" );
			array.accept( walker );
			sqlAppender.appendSql( ')' );
			if ( tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null ) != null ) {
				sqlAppender.appendSql( " with ordinality t(v,i))" );
			}
			else {
				sqlAppender.appendSql( " t(v))" );
			}
		}
	}

	protected void renderXmlTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final XmlHelper.CollectionTags collectionTags = XmlHelper.determineCollectionTags(
				(BasicPluralJavaType<?>) pluralType.getJavaTypeDescriptor(), walker.getSessionFactory()
		);

		sqlAppender.appendSql( "xmltable('/" );
		sqlAppender.appendSql( collectionTags.rootName() );
		sqlAppender.appendSql( '/' );
		sqlAppender.appendSql( collectionTags.elementName() );
		sqlAppender.appendSql( "' passing " );
		array.accept( walker );
		sqlAppender.appendSql( " columns" );
		renderXmlTableColumns( sqlAppender, tupleType, walker );
		sqlAppender.appendSql( ')' );
	}
}
