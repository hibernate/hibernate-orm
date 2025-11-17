/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Christian Beikov
 */
public class StandardUserDefinedTypeExporter implements Exporter<UserDefinedType> {
	protected final Dialect dialect;

	public StandardUserDefinedTypeExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(
			UserDefinedType userDefinedType,
			Metadata metadata,
			SqlStringGenerationContext context) {
		if ( userDefinedType instanceof UserDefinedObjectType userDefinedObjectType ) {
			return getSqlCreateStrings( userDefinedObjectType, metadata, context );
		}
		else if ( userDefinedType instanceof UserDefinedArrayType userDefinedArrayType ) {
			return getSqlCreateStrings( userDefinedArrayType, metadata, context );
		}
		else {
			throw new IllegalArgumentException( "Unsupported user-defined type: " + userDefinedType );
		}
	}

	public String[] getSqlCreateStrings(
			UserDefinedObjectType userDefinedType,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final var typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);

		try {
			final String formattedTypeName = context.format( typeName );
			final var createType =
					new StringBuilder( "create type " )
							.append( formattedTypeName )
							.append( " as " )
							.append( dialect.getCreateUserDefinedTypeKindString() )
							.append( '(' );
			boolean isFirst = true;
			for ( var col : userDefinedType.getColumns() ) {
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					createType.append( ", " );
				}
				createType.append( col.getQuotedName( dialect ) );
				createType.append( ' ' ).append( col.getSqlType( metadata ) );
			}
			createType.append( ')' );
			applyUserDefinedTypeExtensionsString( createType );

			List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( createType.toString() );
			applyComments( userDefinedType, formattedTypeName, sqlStrings );
			return sqlStrings.toArray(StringHelper.EMPTY_STRINGS);
		}
		catch (Exception e) {
			throw new MappingException( "Error creating SQL create commands for UDT : " + typeName, e );
		}
	}

	public String[] getSqlCreateStrings(
			UserDefinedArrayType userDefinedType,
			Metadata metadata,
			SqlStringGenerationContext context) {
		throw new IllegalArgumentException( "Exporter does not support name array types. Can't generate create strings for: " + userDefinedType );
	}

	/**
	 * @param udt The UDT.
	 * @param formattedTypeName The formatted UDT name.
	 * @param sqlStrings The list of SQL strings to add comments to.
	 */
	protected void applyComments(UserDefinedObjectType udt, String formattedTypeName, List<String> sqlStrings) {
		if ( dialect.supportsCommentOn() ) {
			if ( udt.getComment() != null ) {
				sqlStrings.add( "comment on type " + formattedTypeName + " is '" + udt.getComment() + "'" );
			}
			for ( var column : udt.getColumns() ) {
				final String columnComment = column.getComment();
				if ( columnComment != null ) {
					sqlStrings.add( "comment on column " + formattedTypeName + '.' + column.getQuotedName( dialect ) + " is '" + columnComment + "'" );
				}
			}
		}
	}

	protected void applyUserDefinedTypeExtensionsString(StringBuilder buf) {
		buf.append( dialect.getCreateUserDefinedTypeExtensionsString() );
	}

	@Override
	public String[] getSqlDropStrings(UserDefinedType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		if ( userDefinedType instanceof UserDefinedObjectType userDefinedObjectType ) {
			return getSqlDropStrings( userDefinedObjectType, metadata, context );
		}
		else if ( userDefinedType instanceof UserDefinedArrayType userDefinedArrayType ) {
			return getSqlDropStrings( userDefinedArrayType, metadata, context );
		}
		else {
			throw new IllegalArgumentException( "Unsupported user-defined type: " + userDefinedType );
		}
	}

	public String[] getSqlDropStrings(UserDefinedObjectType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		final var dropType = new StringBuilder( "drop type " );
		if ( dialect.supportsIfExistsBeforeTypeName() ) {
			dropType.append( "if exists " );
		}
		final var typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);
		dropType.append( context.format( typeName ) );
		if ( dialect.supportsIfExistsAfterTypeName() ) {
			dropType.append( " if exists" );
		}
		return new String[] { dropType.toString() };
	}

	public String[] getSqlDropStrings(UserDefinedArrayType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		throw new IllegalArgumentException( "Exporter does not support name array types. Can't generate drop strings for: " + userDefinedType );
	}
}
