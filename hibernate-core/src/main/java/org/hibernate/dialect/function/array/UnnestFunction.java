/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.dialect.function.UnnestSetReturningFunctionTypeResolver;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.LazySessionWrapperOptions;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Standard unnest function.
 */
public class UnnestFunction extends AbstractSqmSelfRenderingSetReturningFunctionDescriptor {

	public UnnestFunction(@Nullable String defaultBasicArrayColumnName) {
		this( new UnnestSetReturningFunctionTypeResolver( defaultBasicArrayColumnName ) );
	}

	protected UnnestFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver) {
		super(
				"unnest",
				null,
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
		final @Nullable SqlTypedMapping sqlTypedMapping = array.getExpressionType() instanceof SqlTypedMapping
				? (SqlTypedMapping) array.getExpressionType()
				: null;
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

	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final BasicType<?> elementType = pluralType.getElementType();
		final String columnType = walker.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry().getTypeName(
				elementType.getJdbcType().getDdlTypeCode(),
				sqlTypedMapping == null ? Size.nil() : sqlTypedMapping.toSize(),
				elementType
		);
		sqlAppender.appendSql( "json_table(" );
		array.accept( walker );
		sqlAppender.appendSql( ",'$[*]' columns(" );
		if ( tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null ) == null ) {
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( selectionIndex == 0 ) {
					sqlAppender.append( ' ' );
				}
				else {
					sqlAppender.append( ',' );
				}
				sqlAppender.append( selectableMapping.getSelectableName() );
				sqlAppender.append( ' ' );
				sqlAppender.append( selectableMapping.getColumnDefinition() );
			} );
		}
		else {
			sqlAppender.append( tupleType.getColumnNames().get( 0 ) );
			sqlAppender.appendSql( ' ' );
			sqlAppender.append( columnType );
			sqlAppender.appendSql( " path '$'" );
		}
		sqlAppender.appendSql( "))" );
	}

	protected void renderXmlTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final BasicType<?> elementType = pluralType.getElementType();
		final String columnType = walker.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry().getTypeName(
				elementType.getJdbcType().getDdlTypeCode(),
				sqlTypedMapping == null ? Size.nil() : sqlTypedMapping.toSize(),
				elementType
		);
		sqlAppender.appendSql( "xmltable('$d/" );
		//noinspection unchecked
		final String emptyXml = walker.getSessionFactory().getSessionFactoryOptions().getXmlFormatMapper().toString(
				pluralType.getJavaTypeDescriptor().fromString( "{}" ),
				(JavaType<Object>) pluralType.getJavaTypeDescriptor(),
				new LazySessionWrapperOptions( walker.getSessionFactory() )
		);
		final String rootTag = emptyXml.substring( emptyXml.lastIndexOf( "<" ) + 1, emptyXml.lastIndexOf( "/" ) );
		sqlAppender.appendSql( rootTag );
		sqlAppender.appendSql( "/item' passing " );
		array.accept( walker );
		sqlAppender.appendSql( " as \"d\" columns" );
		char separator = ' ';
		final List<String> columnNames = tupleType.getColumnNames();
		for ( int i = 0; i < columnNames.size(); i++ ) {
			sqlAppender.appendSql( separator );
			sqlAppender.appendSql( columnNames.get( i ) );
			sqlAppender.appendSql( ' ' );
			sqlAppender.appendSql( columnType );
			sqlAppender.appendSql( " path '" );
			sqlAppender.appendSql( "text()" );
			sqlAppender.appendSql( "'" );
			separator = ',';
		}

		sqlAppender.appendSql( ')' );
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
	}
}
