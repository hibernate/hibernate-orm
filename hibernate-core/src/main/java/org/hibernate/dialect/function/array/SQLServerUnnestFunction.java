/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.LazySessionWrapperOptions;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * SQL Server unnest function.
 */
public class SQLServerUnnestFunction extends UnnestFunction {

	public SQLServerUnnestFunction() {
		super( "v" );
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
		final BasicType<?> elementType = pluralType.getElementType();
		final String columnType = walker.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry().getTypeName(
				elementType.getJdbcType().getDdlTypeCode(),
				sqlTypedMapping == null ? Size.nil() : sqlTypedMapping.toSize(),
				elementType
		);
		sqlAppender.appendSql( "openjson(" );
		array.accept( walker );
		sqlAppender.appendSql( ",'$[*]') with (" );
		sqlAppender.append( tupleType.getColumnNames().get( 0 ) );
		sqlAppender.appendSql( ' ' );
		sqlAppender.append( columnType );
		sqlAppender.appendSql( " path '$')" );
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
		final BasicType<?> elementType = pluralType.getElementType();
		final String columnType = walker.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry().getTypeName(
				elementType.getJdbcType().getDdlTypeCode(),
				sqlTypedMapping == null ? Size.nil() : sqlTypedMapping.toSize(),
				elementType
		);
		sqlAppender.appendSql( "(select" );
		char separator = ' ';
		final List<String> columnNames = tupleType.getColumnNames();
		for ( int i = 0; i < columnNames.size(); i++ ) {
			sqlAppender.appendSql( separator );
			sqlAppender.appendSql( " t.v.value('text()[1]','" );
			sqlAppender.appendSql( columnType );
			sqlAppender.appendSql( "') " );
			sqlAppender.appendSql( columnNames.get( i ) );
			separator = ',';
		}

		sqlAppender.appendSql( " from " );
		array.accept( walker );
		sqlAppender.appendSql( ".nodes('/" );
		//noinspection unchecked
		final String emptyXml = walker.getSessionFactory().getSessionFactoryOptions().getXmlFormatMapper().toString(
				pluralType.getJavaTypeDescriptor().fromString( "{}" ),
				(JavaType<Object>) pluralType.getJavaTypeDescriptor(),
				new LazySessionWrapperOptions( walker.getSessionFactory() )
		);
		final String rootTag = emptyXml.substring( emptyXml.lastIndexOf( "<" ) + 1, emptyXml.lastIndexOf( "/" ) );
		sqlAppender.appendSql( rootTag );
		sqlAppender.appendSql( "/item') t(v))" );
	}
}
