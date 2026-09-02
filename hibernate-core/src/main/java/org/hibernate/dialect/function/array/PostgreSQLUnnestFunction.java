/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.spi.AggregateComponentReadRequest;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.spi.query.SetReturningFunctionType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;


/**
 * PostgreSQL unnest function.
 */
public class PostgreSQLUnnestFunction extends UnnestFunction {

	private final boolean supportsJsonTable;

	public PostgreSQLUnnestFunction(boolean supportsJsonTable) {
		super( null, "ordinality", false );
		this.supportsJsonTable = supportsJsonTable;
	}

	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			SetReturningFunctionType tupleType,
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
					sqlAppender.append( "t.i" );
				}
				else if ( CollectionPart.Nature.ELEMENT.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( "t.v" );
				}
				else {
					sqlAppender.append( aggregateSupport.aggregateComponentCustomReadExpression(
							new AggregateComponentReadRequest(
									"",
									"",
									"t.v",
									selectableMapping.getSelectableName(),
									SqlTypes.JSON,
									selectableMapping,
									walker.getSessionFactory().getTypeConfiguration()
							)
					) );
				}
				sqlAppender.append( " as " );
				sqlAppender.append( selectableMapping.getSelectionExpression() );
			} );
			final ModelPart elementPart = tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName() );
			if ( elementPart != null && elementPart.getSingleJdbcMapping().getJdbcType().isStringLike() ) {
				sqlAppender.appendSql( " from jsonb_array_elements_text(" );
			}
			else {
				sqlAppender.appendSql( " from jsonb_array_elements(" );
			}
			array.accept( walker );
			sqlAppender.appendSql( ')' );
			if ( tupleType.findSubPart( CollectionPart.Nature.INDEX.getName() ) != null ) {
				sqlAppender.appendSql( " with ordinality t(v,i))" );
			}
			else {
				sqlAppender.appendSql( " t(v))" );
			}
		}
	}

	@org.hibernate.SPI(org.hibernate.SPI.Role.USE)
	protected void renderXmlTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			SetReturningFunctionType tupleType,
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
