/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.IllegalIdentifierException;
import org.hibernate.internal.util.StringHelper;

/**
 * Parses a qualified name.
 *
 * @author Steve Ebersole
 */
public class QualifiedNameParser {
	/**
	 * Singleton access
	 */
	public static final QualifiedNameParser INSTANCE = new QualifiedNameParser();

	public static class NameParts implements QualifiedName {
		private final Identifier catalogName;
		private final Identifier schemaName;
		private final Identifier objectName;

		private final String qualifiedText;

		public NameParts(Identifier catalogName, Identifier schemaName, Identifier objectName) {
			if ( objectName == null ) {
				throw new IllegalArgumentException( "Name cannot be null" );
			}

			this.catalogName = catalogName;
			this.schemaName = schemaName;
			this.objectName = objectName;

			StringBuilder buff = new StringBuilder();
			if ( catalogName != null ) {
				buff.append( catalogName ).append( '.' );
			}
			if ( schemaName != null ) {
				buff.append( schemaName ).append( '.' );
			}
			buff.append( objectName );
			qualifiedText = buff.toString();
		}

		@Override
		public Identifier getCatalogName() {
			return catalogName;
		}

		@Override
		public Identifier getSchemaName() {
			return schemaName;
		}

		@Override
		public Identifier getObjectName() {
			return objectName;
		}

		@Override
		public String render() {
			return qualifiedText;
		}

		@Override
		public String toString() {
			return qualifiedText;
		}

		@Override
		@SuppressWarnings("SimplifiableIfStatement")
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			NameParts that = (NameParts) o;

			return Objects.equals( this.getCatalogName(), that.getCatalogName() )
				&& Objects.equals( this.getSchemaName(), that.getSchemaName() )
				&& Objects.equals( this.getObjectName(), that.getObjectName() );
		}

		@Override
		public int hashCode() {
			int result = getCatalogName() != null ? getCatalogName().hashCode() : 0;
			result = 31 * result + ( getSchemaName() != null ? getSchemaName().hashCode() : 0);
			result = 31 * result + getObjectName().hashCode();
			return result;
		}
	}

	/**
	 * Parses a textual representation of a qualified name into a NameParts
	 * representation.  Explicitly looks for the form {@code catalog.schema.name}.
	 *
	 * @param text The simple text representation of the qualified name.
	 *
	 * @return The wrapped QualifiedName
	 */
	public NameParts parse(String text, Identifier defaultCatalog, Identifier defaultSchema) {
		if ( text == null ) {
			throw new IllegalIdentifierException( "Object name to parse must be specified, but found null" );
		}

		final int quoteCharCount = StringHelper.count( text, "`" );
		final boolean wasQuotedInEntirety = quoteCharCount == 2 && text.startsWith( "`" ) && text.endsWith( "`" );

		if ( wasQuotedInEntirety ) {
			return new NameParts(
					defaultCatalog,
					defaultSchema,
					Identifier.toIdentifier( unquote( text ), true )
			);
		}

		String catalogName = null;
		String schemaName = null;
		String name;

		boolean catalogWasQuoted = false;
		boolean schemaWasQuoted = false;
		boolean nameWasQuoted;

		final String[] tokens = StringHelper.split( ".", text );
		if ( tokens.length == 0 || tokens.length == 1 ) {
			// we have just a local name...
			name = text;
		}
		else if ( tokens.length == 2 ) {
			schemaName = tokens[0];
			name = tokens[1];
		}
		else if ( tokens.length == 3 ) {
			schemaName = tokens[0];
			catalogName = tokens[1];
			name = tokens[2];
		}
		else {
			throw new HibernateException( "Unable to parse object name: " + text );
		}

		nameWasQuoted = Identifier.isQuoted( name );
		if ( nameWasQuoted ) {
			name = unquote( name );
		}

		if ( schemaName != null ) {
			schemaWasQuoted = Identifier.isQuoted( schemaName );
			if ( schemaWasQuoted ) {
				schemaName = unquote( schemaName );
			}
		}
		else if ( defaultSchema != null ) {
			schemaName = defaultSchema.getText();
			schemaWasQuoted = defaultSchema.isQuoted();
		}

		if ( catalogName != null ) {
			catalogWasQuoted = Identifier.isQuoted( catalogName );
			if ( catalogWasQuoted ) {
				catalogName = unquote( catalogName );
			}
		}
		else if ( defaultCatalog != null ) {
			catalogName = defaultCatalog.getText();
			catalogWasQuoted = defaultCatalog.isQuoted();
		}

		return new NameParts(
				Identifier.toIdentifier( catalogName, wasQuotedInEntirety||catalogWasQuoted ),
				Identifier.toIdentifier( schemaName, wasQuotedInEntirety||schemaWasQuoted ),
				Identifier.toIdentifier( name, wasQuotedInEntirety||nameWasQuoted )
		);
	}

	private static String unquote(String text) {
		return text.substring( 1, text.length() - 1 );
	}

	/**
	 * Parses a textual representation of a qualified name into a NameParts
	 * representation.  Explicitly looks for the form {@code catalog.schema.name}.
	 *
	 * @param text The simple text representation of the qualified name.
	 *
	 * @return The wrapped QualifiedName
	 */
	public NameParts parse(String text) {
		return parse( text, null, null );
	}
}
