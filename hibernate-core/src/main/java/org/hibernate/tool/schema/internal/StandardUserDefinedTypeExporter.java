/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
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
		if ( userDefinedType instanceof UserDefinedObjectType ) {
			return getSqlCreateStrings( (UserDefinedObjectType) userDefinedType, metadata, context );
		}
		else if ( userDefinedType instanceof UserDefinedArrayType ) {
			return getSqlCreateStrings( (UserDefinedArrayType) userDefinedType, metadata, context );
		}
		else {
			throw new IllegalArgumentException( "Unsupported user-defined type: " + userDefinedType );
		}
	}

	public String[] getSqlCreateStrings(
			UserDefinedObjectType userDefinedType,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final QualifiedName typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);

		try {
			String formattedTypeName = context.format( typeName );
			StringBuilder buf =
					new StringBuilder( "create type " )
							.append( formattedTypeName )
							.append( " as " )
							.append( dialect.getCreateUserDefinedTypeKindString() )
							.append( '(' );

			boolean isFirst = true;
			for ( Column col : userDefinedType.getColumns() ) {
				if ( isFirst ) {
					isFirst = false;
				}
				else {
					buf.append( ", " );
				}

				String colName = col.getQuotedName( dialect );
				buf.append( colName );

				buf.append( ' ' ).append( col.getSqlType( metadata ) );
			}

			buf.append( ')' );

			applyUserDefinedTypeExtensionsString( buf );

			List<String> sqlStrings = new ArrayList<>();
			sqlStrings.add( buf.toString() );

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
			for ( Column column : udt.getColumns() ) {
				String columnComment = column.getComment();
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
		if ( userDefinedType instanceof UserDefinedObjectType ) {
			return getSqlDropStrings( (UserDefinedObjectType) userDefinedType, metadata, context );
		}
		else if ( userDefinedType instanceof UserDefinedArrayType ) {
			return getSqlDropStrings( (UserDefinedArrayType) userDefinedType, metadata, context );
		}
		else {
			throw new IllegalArgumentException( "Unsupported user-defined type: " + userDefinedType );
		}
	}

	public String[] getSqlDropStrings(UserDefinedObjectType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		StringBuilder buf = new StringBuilder( "drop type " );
		if ( dialect.supportsIfExistsBeforeTypeName() ) {
			buf.append( "if exists " );
		}

		final QualifiedName typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);
		buf.append( context.format( typeName ) );

		if ( dialect.supportsIfExistsAfterTypeName() ) {
			buf.append( " if exists" );
		}

		return new String[] { buf.toString() };
	}

	public String[] getSqlDropStrings(UserDefinedArrayType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		throw new IllegalArgumentException( "Exporter does not support name array types. Can't generate drop strings for: " + userDefinedType );
	}
}
