/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.UserDefinedType;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
/// Stock exporter for relational [UserDefinedType] definitions.
///
/// Instantiate this class when standard object-type DDL is sufficient. Compose
/// or implement [Exporter] when a database has a different user-defined-type
/// algorithm, and supply the result from
/// [Dialect#getUserDefinedTypeExporter()].
///
/// @see Dialect#getUserDefinedTypeExporter()
///
/// @author Steve Ebersole
/// @author Christian Beikov
@SPI({ USE, SUPPLY })
public final class StandardUserDefinedTypeExporter implements Exporter<UserDefinedType> {
	private final Dialect dialect;
	private final UserDefinedTypeDdlSupport ddlSupport;

	/// Create a standard user-defined-type exporter owned by `dialect`.
	public StandardUserDefinedTypeExporter(Dialect dialect) {
		this( dialect, UserDefinedTypeDdlSupport.STANDARD );
	}

	/// Create a standard exporter with explicit immutable DDL grammar.
	///
	/// @since 8.0
	public StandardUserDefinedTypeExporter(Dialect dialect, UserDefinedTypeDdlSupport ddlSupport) {
		if ( dialect == null || ddlSupport == null ) {
			throw new IllegalArgumentException( "Dialect and UDT DDL support must not be null" );
		}
		this.dialect = dialect;
		this.ddlSupport = ddlSupport;
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

	private String[] getSqlCreateStrings(
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
							.append( ddlSupport.createTypeKind() )
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
			createType.append( ddlSupport.createTypeExtensions() );

			List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( createType.toString() );
			applyComments( userDefinedType, formattedTypeName, sqlStrings );
			return sqlStrings.toArray(StringHelper.EMPTY_STRINGS);
		}
		catch (Exception e) {
			throw new MappingException( "Error creating SQL create commands for UDT : " + typeName, e );
		}
	}

	private String[] getSqlCreateStrings(
			UserDefinedArrayType userDefinedType,
			Metadata metadata,
			SqlStringGenerationContext context) {
		throw new IllegalArgumentException( "Exporter does not support name array types. Can't generate create strings for: " + userDefinedType );
	}

	private void applyComments(UserDefinedObjectType udt, String formattedTypeName, List<String> sqlStrings) {
		final var support = dialect.getSchemaCommentSupport();
		final String comment = udt.getComment();
		if ( comment != null
				&& support.placement( CommentTarget.USER_DEFINED_TYPE ) == CommentPlacement.STATEMENT ) {
			sqlStrings.add( support.render( new CommentRequest(
					CommentTarget.USER_DEFINED_TYPE,
					formattedTypeName,
					comment
			) ) );
		}
		if ( support.placement( CommentTarget.USER_DEFINED_TYPE_COLUMN ) == CommentPlacement.STATEMENT ) {
			for ( var column : udt.getColumns() ) {
				final String columnComment = column.getComment();
				if ( columnComment != null ) {
					sqlStrings.add( support.render( new CommentRequest(
							CommentTarget.USER_DEFINED_TYPE_COLUMN,
							formattedTypeName + '.' + column.getQuotedName( dialect ),
							columnComment
					) ) );
				}
			}
		}
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

	private String[] getSqlDropStrings(UserDefinedObjectType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		final var dropType = new StringBuilder( "drop type " );
		if ( ddlSupport.dropIfExistsPlacement() == ExistenceCheckPlacement.BEFORE_NAME ) {
			dropType.append( "if exists " );
		}
		final var typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);
		dropType.append( context.format( typeName ) );
		if ( ddlSupport.dropIfExistsPlacement() == ExistenceCheckPlacement.AFTER_NAME ) {
			dropType.append( " if exists" );
		}
		return new String[] { dropType.toString() };
	}

	private String[] getSqlDropStrings(UserDefinedArrayType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		throw new IllegalArgumentException( "Exporter does not support name array types. Can't generate drop strings for: " + userDefinedType );
	}
}
