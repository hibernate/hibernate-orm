/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.dialect.function.UnnestSetReturningFunctionTypeResolver;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Standard unnest function.
 */
public class UnnestFunction extends AbstractSqmSelfRenderingSetReturningFunctionDescriptor {

	public UnnestFunction(@Nullable String defaultBasicArrayColumnName, String defaultIndexSelectionExpression) {
		this( new UnnestSetReturningFunctionTypeResolver( defaultBasicArrayColumnName, defaultIndexSelectionExpression ) );
	}

	protected UnnestFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver) {
		super(
				"unnest",
				ArrayArgumentValidator.DEFAULT_INSTANCE,
				setReturningFunctionTypeResolver,
				null
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final Expression array = (Expression) sqlAstArguments.get( 0 );
		final @Nullable SqlTypedMapping sqlTypedMapping =
				array.getExpressionType() instanceof SqlTypedMapping sqlTyped ? sqlTyped : null;
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) array.getExpressionType().getSingleJdbcMapping();
		final int ddlTypeCode = pluralType.getJdbcType().getDefaultSqlTypeCode();
		if ( ddlTypeCode == SqlTypes.JSON_ARRAY ) {
			renderJsonTable( sqlAppender, array, pluralType, sqlTypedMapping, tupleType, tableIdentifierVariable, walker );
		}
		else if ( ddlTypeCode == SqlTypes.XML_ARRAY ) {
			renderXmlTable( sqlAppender, array, pluralType, sqlTypedMapping, tupleType, tableIdentifierVariable, walker );
		}
		else {
			renderUnnest( sqlAppender, array, pluralType, sqlTypedMapping, tupleType, tableIdentifierVariable, walker );
		}
	}

	protected String getDdlType(SqlTypedMapping sqlTypedMapping, int containerSqlTypeCode, SqlAstTranslator<?> translator) {
		final String columnDefinition = sqlTypedMapping.getColumnDefinition();
		if ( columnDefinition != null ) {
			return columnDefinition;
		}
		return translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry().getTypeName(
				sqlTypedMapping.getJdbcMapping().getJdbcType().getDdlTypeCode(),
				sqlTypedMapping.toSize(),
				(Type) sqlTypedMapping.getJdbcMapping()
		);
	}

	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_table(" );
		array.accept( walker );
		sqlAppender.appendSql( ",'$[*]' columns" );
		renderJsonTableColumns( sqlAppender, tupleType, walker, false );
		sqlAppender.appendSql( ')' );
	}

	protected void renderJsonTableColumns(SqlAppender sqlAppender, AnonymousTupleTableGroupProducer tupleType, SqlAstTranslator<?> walker, boolean errorOnError) {
		if ( tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null ) == null ) {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( '(' );
				}
				else {
					sqlAppender.append( ',' );
				}
				sqlAppender.append( selectableMapping.getSelectionExpression() );
				sqlAppender.append( ' ' );
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( " for ordinality" );
				}
				else {
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.JSON_ARRAY, walker ) );
					sqlAppender.appendSql( " path '$." );
					sqlAppender.append( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( '\'' );
					if ( errorOnError ) {
						sqlAppender.appendSql( " error on error" );
					}
				}
			} );
		}
		else {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( '(' );
				}
				else {
					sqlAppender.append( ',' );
				}
				sqlAppender.append( selectableMapping.getSelectionExpression() );
				if ( CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					sqlAppender.append( " for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.JSON_ARRAY, walker ) );
					sqlAppender.appendSql( " path '$'" );
					if ( errorOnError ) {
						sqlAppender.appendSql( " error on error" );
					}
				}
			} );
		}
		sqlAppender.appendSql( ')' );
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

		sqlAppender.appendSql( "xmltable('$d/" );
		sqlAppender.appendSql( collectionTags.rootName() );
		sqlAppender.appendSql( '/' );
		sqlAppender.appendSql( collectionTags.elementName() );
		sqlAppender.appendSql( "' passing " );
		array.accept( walker );
		sqlAppender.appendSql( " as \"d\" columns" );
		renderXmlTableColumns( sqlAppender, tupleType, walker );
		sqlAppender.appendSql( ')' );
	}

	protected void renderXmlTableColumns(SqlAppender sqlAppender, AnonymousTupleTableGroupProducer tupleType, SqlAstTranslator<?> walker) {
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
					sqlAppender.append( " for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( " path '" );
					sqlAppender.appendSql( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( "/text()" );
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
					sqlAppender.append( " for ordinality" );
				}
				else {
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.XML_ARRAY, walker ) );
					sqlAppender.appendSql( " path '" );
					sqlAppender.appendSql( "text()" );
					sqlAppender.appendSql( "'" );
				}
			} );
		}
	}

	protected void renderUnnest(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "unnest(" );
		array.accept( walker );
		sqlAppender.appendSql( ')' );
		if ( tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null ) != null ) {
			sqlAppender.append( " with ordinality" );
		}
	}
}
