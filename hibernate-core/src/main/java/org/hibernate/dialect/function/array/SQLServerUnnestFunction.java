/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * SQL Server unnest function.
 */
public class SQLServerUnnestFunction extends UnnestFunction {

	public SQLServerUnnestFunction() {
		super( "v", "i" );
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
		final ModelPart ordinalityPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		if ( ordinalityPart != null ) {
			sqlAppender.appendSql( "(select t.*,row_number() over (order by (select null)) " );
			sqlAppender.appendSql( ordinalityPart.asBasicValuedModelPart().getSelectionExpression() );
			sqlAppender.appendSql( " from openjson(" );
		}
		else {
			sqlAppender.appendSql( "openjson(" );
		}
		array.accept( walker );
		sqlAppender.appendSql( ") with (" );

		boolean[] comma = new boolean[1];
		if ( tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null ) == null ) {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( !CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					if ( comma[0] ) {
						sqlAppender.append( ',' );
					}
					else {
						sqlAppender.append( ' ' );
						comma[0] = true;
					}
					sqlAppender.append( selectableMapping.getSelectionExpression() );
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.JSON_ARRAY, walker ) );
					sqlAppender.appendSql( " '$." );
					sqlAppender.append( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( '\'' );
				}
			} );
		}
		else {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( !CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					if ( comma[0] ) {
						sqlAppender.append( ',' );
					}
					else {
						sqlAppender.append( ' ' );
						comma[0] = true;
					}
					sqlAppender.append( selectableMapping.getSelectionExpression() );
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.JSON_ARRAY, walker ) );
					sqlAppender.appendSql( " '$'" );
				}
			} );
		}

		if ( ordinalityPart != null ) {
			sqlAppender.appendSql( ")t)" );
		}
		else {
			sqlAppender.appendSql( ')' );
		}
	}

	@Override
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

		sqlAppender.appendSql( "(select" );

		if ( tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null ) == null ) {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( ' ' );
				}
				else {
					sqlAppender.append( ',' );
				}
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.appendSql( "t.v.value('count(for $a in . return $a/../" );
					sqlAppender.appendSql( collectionTags.elementName() );
					sqlAppender.appendSql( "[.<<$a])+1','" );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( "') " );
					sqlAppender.appendSql( selectableMapping.getSelectionExpression() );
				}
				else {
					sqlAppender.appendSql( "t.v.value('(");
					sqlAppender.appendSql( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( "/text())[1]','" );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( "') " );
					sqlAppender.appendSql( selectableMapping.getSelectionExpression() );
				}
			} );
		}
		else {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( ' ' );
				}
				else {
					sqlAppender.append( ',' );
				}
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.appendSql( "t.v.value('count(for $a in . return $a/../" );
					sqlAppender.appendSql( collectionTags.elementName() );
					sqlAppender.appendSql( "[.<<$a])+1','" );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( "') " );
					sqlAppender.appendSql( selectableMapping.getSelectionExpression() );
				}
				else {
					sqlAppender.appendSql( "t.v.value('text()[1]','" );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( "') " );
					sqlAppender.appendSql( selectableMapping.getSelectionExpression() );
				}
			} );
		}

		sqlAppender.appendSql( " from " );
		array.accept( walker );
		sqlAppender.appendSql( ".nodes('/" );
		sqlAppender.appendSql( collectionTags.rootName() );
		sqlAppender.appendSql( '/' );
		sqlAppender.appendSql( collectionTags.elementName() );
		sqlAppender.appendSql( "') t(v))" );
	}
}
