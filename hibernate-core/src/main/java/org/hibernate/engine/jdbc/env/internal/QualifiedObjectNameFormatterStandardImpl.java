/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.internal;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;

/**
 * Standard implementation of {@link QualifiedObjectNameFormatter} which uses information reported
 * by {@link DatabaseMetaData} to render qualified names.
 *
 * @author Steve Ebersole
 */
public class QualifiedObjectNameFormatterStandardImpl implements QualifiedObjectNameFormatter {
	private final Format format;

	public QualifiedObjectNameFormatterStandardImpl(
			NameQualifierSupport nameQualifierSupport,
			String catalogSeparator,
			boolean catalogAtEnd) {
		this.format = buildFormat( nameQualifierSupport, catalogSeparator, catalogAtEnd );
	}

	private Format buildFormat(
			NameQualifierSupport nameQualifierSupport,
			String catalogSeparator,
			boolean catalogAtEnd) {
		return switch ( nameQualifierSupport ) {
			case NONE -> NoQualifierSupportFormat.INSTANCE;
			case CATALOG -> catalogAtEnd
					? new NameCatalogFormat( catalogSeparator )
					: new CatalogNameFormat( catalogSeparator );
			case SCHEMA -> SchemaNameFormat.INSTANCE;
			default -> catalogAtEnd
					? new SchemaNameCatalogFormat( catalogSeparator )
					: new CatalogSchemaNameFormat( catalogSeparator );
		};
	}

	public QualifiedObjectNameFormatterStandardImpl(NameQualifierSupport nameQualifierSupport, String catalogSeparator) {
		// most dbs simply do <catalog>.<schema>.<name>
		this( nameQualifierSupport, catalogSeparator, false );
	}

	public QualifiedObjectNameFormatterStandardImpl(
			NameQualifierSupport nameQualifierSupport,
			DatabaseMetaData databaseMetaData) throws SQLException {
		this(
				nameQualifierSupport,
				databaseMetaData.getCatalogSeparator(),
				!databaseMetaData.isCatalogAtStart()
		);
	}

	@Override
	public String format(QualifiedTableName qualifiedTableName, Dialect dialect) {
		return format.format(
				render( qualifiedTableName.getCatalogName(), dialect ),
				render( qualifiedTableName.getSchemaName(), dialect ),
				render( qualifiedTableName.getTableName(), dialect )
		);
	}

	private static String render(Identifier identifier, Dialect dialect) {
		if ( identifier == null ) {
			return null;
		}

		return identifier.render( dialect );
	}

	@Override
	public String format(QualifiedSequenceName qualifiedSequenceName, Dialect dialect) {
		return format.format(
				render( qualifiedSequenceName.getCatalogName(), dialect ),
				render( qualifiedSequenceName.getSchemaName(), dialect ),
				render( qualifiedSequenceName.getSequenceName(), dialect )
		);
	}

	@Override
	public String format(QualifiedName qualifiedName, Dialect dialect) {
		return format.format(
				render( qualifiedName.getCatalogName(), dialect ),
				render( qualifiedName.getSchemaName(), dialect ),
				render( qualifiedName.getObjectName(), dialect )
		);
	}

	@Override
	public String format(QualifiedPhysicalName name, Dialect dialect) {
		final var support = dialect.getIdentifierSupport();
		return format.format( name.catalogName() == null ? null : support.render( name.catalogName() ),
				name.schemaName() == null ? null : support.render( name.schemaName() ), support.render( name.objectName() ) );
	}

	private interface Format {
		String format(String catalog, String schema, String name);
	}

	private record NoQualifierSupportFormat() implements Format {
		public static final NoQualifierSupportFormat INSTANCE = new NoQualifierSupportFormat();
		@Override
		public String format(String catalog, String schema, String name) {
			return name;
		}
	}

	private record SchemaNameCatalogFormat(String catalogSeparator) implements Format {
		@Override
		public String format(String catalog, String schema, String name) {
			final var formatted = new StringBuilder();
			if ( schema != null ) {
				formatted.append( schema ).append( '.' );
			}
			formatted.append( name );
			if ( catalog != null ) {
				formatted.append( catalogSeparator ).append( catalog );
			}
			return formatted.toString();
		}
	}

	private record CatalogSchemaNameFormat(String catalogSeparator) implements Format {

		@Override
		public String format(String catalog, String schema, String name) {
			final var formatted = new StringBuilder();
			if ( catalog != null ) {
				formatted.append( catalog ).append( catalogSeparator );
			}
			if ( schema != null ) {
				formatted.append( schema ).append( '.' );
			}
			formatted.append( name );
			return formatted.toString();
		}
	}

	private record NameCatalogFormat(String catalogSeparator) implements Format {
		@Override
		public String format(String catalog, String schema, String name) {
			final var formatted = new StringBuilder();
			formatted.append( name );
			if ( catalog != null ) {
				formatted.append( catalogSeparator ).append( catalog );
			}
			return formatted.toString();
		}
	}

	private record CatalogNameFormat(String catalogSeparator) implements Format {
		@Override
			public String format(String catalog, String schema, String name) {
				final var formatted = new StringBuilder();
				if ( catalog != null ) {
					formatted.append( catalog ).append( catalogSeparator );
				}
				formatted.append( name );
				return formatted.toString();
			}
		}

	private record SchemaNameFormat() implements Format {
		/**
		 * Singleton access
		 */
		public static final SchemaNameFormat INSTANCE = new SchemaNameFormat();
		@Override
		public String format(String catalog, String schema, String name) {
			final var formatted = new StringBuilder();
			if ( schema != null ) {
				formatted.append( schema ).append( '.' );
			}
			formatted.append( name );
			return formatted.toString();
		}
	}
}
