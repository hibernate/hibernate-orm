/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
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
 * Sybase ASE unnest function.
 */
public class SybaseASEUnnestFunction extends UnnestFunction {

	public SybaseASEUnnestFunction() {
		super( "v", "i" );
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
		sqlAppender.appendSql( "xmltable('/" );
		sqlAppender.appendSql( collectionTags.rootName() );
		sqlAppender.appendSql( '/' );
		sqlAppender.appendSql( collectionTags.elementName() );
		sqlAppender.appendSql( "' passing " );
		array.accept( walker );
		sqlAppender.appendSql( " columns" );

		if ( tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null ) == null ) {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( ' ' );
				}
				else {
					sqlAppender.append( ',' );
				}
				sqlAppender.append( selectableMapping.getSelectionExpression() );
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( " bigint for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( " path '" );
					sqlAppender.appendSql( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( "'" );
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
				sqlAppender.append( selectableMapping.getSelectionExpression() );
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( " bigint for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( " path '" );
					sqlAppender.appendSql( "." );
					sqlAppender.appendSql( "'" );
				}
			} );
		}

		sqlAppender.appendSql( ')' );
	}
}
