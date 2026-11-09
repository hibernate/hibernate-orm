package org.hibernate.engine.jdbc.env.internal;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	private final @Nonnull Format format;

	public QualifiedObjectNameFormatterStandardImpl(
			@Nonnull NameQualifierSupport nameQualifierSupport,
			@Nonnull String catalogSeparator,
			boolean catalogAtEnd) {
		this.format = buildFormat( nameQualifierSupport, catalogSeparator, catalogAtEnd );
	}

	@Nonnull
	private Format buildFormat(
			@Nonnull NameQualifierSupport nameQualifierSupport,
			@Nonnull String catalogSeparator,
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

	public QualifiedObjectNameFormatterStandardImpl(
			@Nonnull NameQualifierSupport nameQualifierSupport,
			@Nonnull String catalogSeparator) {
		// most dbs simply do <catalog>.<schema>.<name>
		this( nameQualifierSupport, catalogSeparator, false );
	}

	public QualifiedObjectNameFormatterStandardImpl(
			@Nonnull NameQualifierSupport nameQualifierSupport,
			@Nonnull DatabaseMetaData databaseMetaData) throws SQLException {
		this(
				nameQualifierSupport,
				databaseMetaData.getCatalogSeparator(),
				!databaseMetaData.isCatalogAtStart()
		);
	}

	@Override
	@Nonnull
	public String format(@Nonnull QualifiedTableName qualifiedTableName, @Nonnull Dialect dialect) {
		return format.format(
				qualifiedTableName.getCatalogName(),
				qualifiedTableName.getSchemaName(),
				qualifiedTableName.getTableName(),
				dialect
		);
	}

	@Override
	@Nonnull
	public String format(@Nonnull QualifiedSequenceName qualifiedSequenceName, @Nonnull Dialect dialect) {
		return format.format(
				qualifiedSequenceName.getCatalogName(),
				qualifiedSequenceName.getSchemaName(),
				qualifiedSequenceName.getSequenceName(),
				dialect
		);
	}

	@Override
	@Nonnull
	public String format(@Nonnull QualifiedName qualifiedName, @Nonnull Dialect dialect) {
		return format.format(
				qualifiedName.getCatalogName(),
				qualifiedName.getSchemaName(),
				qualifiedName.getObjectName(),
				dialect
		);
	}

	private interface Format {
		@Nonnull
		String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect);
	}

	private record NoQualifierSupportFormat() implements Format {
		public static final NoQualifierSupportFormat INSTANCE = new NoQualifierSupportFormat();
		@Override
		@Nonnull
		public String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect) {
			return name.render( dialect );
		}
	}

	private record SchemaNameCatalogFormat(String catalogSeparator) implements Format {
		@Override
		@Nonnull
		public String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect) {
			final var formatted = new StringBuilder();
			if ( schema != null ) {
				formatted.append( schema.render( dialect ) ).append( '.' );
			}
			formatted.append( name.render( dialect ) );
			if ( catalog != null ) {
				formatted.append( catalogSeparator ).append( catalog.render( dialect ) );
			}
			return formatted.toString();
		}
	}

	private record CatalogSchemaNameFormat(String catalogSeparator) implements Format {

		@Override
		@Nonnull
		public String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect) {
			final var formatted = new StringBuilder();
			if ( catalog != null ) {
				formatted.append( catalog.render( dialect ) ).append( catalogSeparator );
			}
			if ( schema != null ) {
				formatted.append( schema.render( dialect ) ).append( '.' );
			}
			formatted.append( name.render( dialect ) );
			return formatted.toString();
		}
	}

	private record NameCatalogFormat(String catalogSeparator) implements Format {
		@Override
		@Nonnull
		public String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect) {
			final var formatted = new StringBuilder();
			formatted.append( name.render( dialect ) );
			if ( catalog != null ) {
				formatted.append( catalogSeparator ).append( catalog.render( dialect ) );
			}
			return formatted.toString();
		}
	}

	private record CatalogNameFormat(String catalogSeparator) implements Format {
		@Override
		@Nonnull
		public String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect) {
			final var formatted = new StringBuilder();
			if ( catalog != null ) {
				formatted.append( catalog.render( dialect ) ).append( catalogSeparator );
			}
			formatted.append( name.render( dialect ) );
			return formatted.toString();
		}
	}

	private record SchemaNameFormat() implements Format {
		/**
		 * Singleton access
		 */
		public static final SchemaNameFormat INSTANCE = new SchemaNameFormat();
		@Override
		@Nonnull
		public String format(
				@Nullable Identifier catalog,
				@Nullable Identifier schema,
				@Nonnull Identifier name,
				@Nonnull Dialect dialect) {
			final var formatted = new StringBuilder();
			if ( schema != null ) {
				formatted.append( schema.render( dialect ) ).append( '.' );
			}
			formatted.append( name.render( dialect ) );
			return formatted.toString();
		}
	}
}
