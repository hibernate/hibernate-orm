/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.env.internal;

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
 * Standard implementation of QualifiedObjectNameFormatter which uses information reported
 * by {@link java.sql.DatabaseMetaData} to render qualified names.
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
		switch ( nameQualifierSupport ) {
			case NONE: {
				return NoQualifierSupportFormat.INSTANCE;
			}
			case CATALOG: {
				return catalogAtEnd
						? new NameCatalogFormat( catalogSeparator )
						: new CatalogNameFormat( catalogSeparator );
			}
			case SCHEMA: {
				return SchemaNameFormat.INSTANCE;
			}
			default: {
				return catalogAtEnd
						? new SchemaNameCatalogFormat( catalogSeparator )
						: new CatalogSchemaNameFormat( catalogSeparator );
			}
		}
	}

	public QualifiedObjectNameFormatterStandardImpl(NameQualifierSupport nameQualifierSupport) {
		// most dbs simply do <catalog>.<schema>.<name>
		this( nameQualifierSupport, ".", false );
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
				qualifiedTableName.getCatalogName(),
				qualifiedTableName.getSchemaName(),
				qualifiedTableName.getTableName(),
				dialect
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
				qualifiedSequenceName.getCatalogName(),
				qualifiedSequenceName.getSchemaName(),
				qualifiedSequenceName.getSequenceName(),
				dialect
		);
	}

	@Override
	public String format(QualifiedName qualifiedName, Dialect dialect) {
		return format.format(
				qualifiedName.getCatalogName(),
				qualifiedName.getSchemaName(),
				qualifiedName.getObjectName(),
				dialect
		);
	}

	private static interface Format {
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect);
	}

	private static class NoQualifierSupportFormat implements Format {
		/**
		 * Singleton access
		 */
		public static final NoQualifierSupportFormat INSTANCE = new NoQualifierSupportFormat();

		@Override
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect) {
			return render( name, dialect );
		}
	}

	private static class SchemaNameCatalogFormat implements Format {
		private final String catalogSeparator;

		public SchemaNameCatalogFormat(String catalogSeparator) {
			this.catalogSeparator = catalogSeparator;
		}

		@Override
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect) {
			StringBuilder buff = new StringBuilder();
			if ( schema != null ) {
				buff.append( render( schema, dialect ) ).append( '.' );
			}

			buff.append( render( name, dialect ) );

			if ( catalog != null ) {
				buff.append( catalogSeparator ).append( render( catalog, dialect ) );
			}

			return buff.toString();
		}
	}

	private static class CatalogSchemaNameFormat implements Format {
		private final String catalogSeparator;

		public CatalogSchemaNameFormat(String catalogSeparator) {
			this.catalogSeparator = catalogSeparator;
		}

		@Override
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect) {
			StringBuilder buff = new StringBuilder();

			if ( catalog != null ) {
				buff.append( render( catalog, dialect ) ).append( catalogSeparator );
			}

			if ( schema != null ) {
				buff.append( render( schema, dialect ) ).append( '.' );
			}

			buff.append( render( name, dialect ) );

			return buff.toString();
		}
	}

	private static class NameCatalogFormat implements Format {
		private final String catalogSeparator;

		public NameCatalogFormat(String catalogSeparator) {
			this.catalogSeparator = catalogSeparator;
		}

		@Override
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect) {
			StringBuilder buff = new StringBuilder();

			buff.append( render( name, dialect ) );

			if ( catalog != null ) {
				buff.append( catalogSeparator ).append( render( catalog, dialect ) );
			}

			return buff.toString();
		}
	}

	private static class CatalogNameFormat implements Format {
		private final String catalogSeparator;

		public CatalogNameFormat(String catalogSeparator) {
			this.catalogSeparator = catalogSeparator;
		}

		@Override
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect) {
			StringBuilder buff = new StringBuilder();

			if ( catalog != null ) {
				buff.append( render( catalog, dialect ) ).append( catalogSeparator );
			}

			buff.append( render( name, dialect ) );

			return buff.toString();
		}
	}

	private static class SchemaNameFormat implements Format {
		/**
		 * Singleton access
		 */
		public static final SchemaNameFormat INSTANCE = new SchemaNameFormat();

		@Override
		public String format(Identifier catalog, Identifier schema, Identifier name, Dialect dialect) {
			StringBuilder buff = new StringBuilder();

			if ( schema != null ) {
				buff.append( render( schema, dialect ) ).append( '.' );
			}

			buff.append( render( name, dialect ) );

			return buff.toString();
		}
	}
}
