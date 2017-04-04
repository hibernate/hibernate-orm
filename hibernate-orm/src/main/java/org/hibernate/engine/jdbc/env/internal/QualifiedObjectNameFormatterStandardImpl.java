/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
