/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.internal;

import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Selectable;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.StandardIndexExporter;
import org.hibernate.type.SqlTypes;

import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.qualify;

/// Specialized index exporter for Cloud Spanner GoogleSQL ([SpannerDialect])
/// supporting vector indexes for approximate nearest neighbor (ANN) similarity search.
///
/// GoogleSQL uses `create vector index <name> on <table> (<col>) [where <col> is not null] options (...)`
/// and `drop vector index <name>`.
///
/// Standard non-vector indexes are delegated to [StandardIndexExporter].
public class SpannerIndexExporter implements Exporter<Index> {

	private final Dialect dialect;
	private final StandardIndexExporter standardExporter;

	public SpannerIndexExporter(SpannerDialect dialect) {
		this( (Dialect) dialect );
	}

	public SpannerIndexExporter(Dialect dialect) {
		this.dialect = dialect;
		this.standardExporter = new StandardIndexExporter( dialect );
	}

	public boolean isVectorIndex(Index index, Metadata metadata) {
		if ( index == null ) {
			return false;
		}
		if ( "vector".equalsIgnoreCase( index.getType() ) || "vector".equalsIgnoreCase( index.getUsing() ) ) {
			return true;
		}
		for ( Selectable selectable : index.getSelectables() ) {
			if ( selectable instanceof Column col ) {
				Integer code = col.getSqlTypeCode();
				if ( code == null && metadata != null ) {
					try {
						code = col.getSqlTypeCode( metadata );
					}
					catch (Exception ignored) {
					}
				}
				if ( code != null && isVectorSqlType( code ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isVectorSqlType(int code) {
		return code == SqlTypes.VECTOR
				|| code == SqlTypes.VECTOR_FLOAT32
				|| code == SqlTypes.VECTOR_FLOAT64
				|| code == SqlTypes.VECTOR_INT8
				|| code == SqlTypes.VECTOR_BINARY
				|| code == SqlTypes.VECTOR_FLOAT16;
	}

	@Override
	public String[] getSqlCreateStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
		if ( isVectorIndex( index, metadata ) ) {
			return new String[] { buildVectorIndexDdl( index, metadata, context ) };
		}
		return standardExporter.getSqlCreateStrings( index, metadata, context );
	}

	@Override
	public String[] getSqlDropStrings(Index index, Metadata metadata, SqlStringGenerationContext context) {
		final String[] dropStrings = standardExporter.getSqlDropStrings( index, metadata, context );
		if ( isVectorIndex( index, metadata ) ) {
			for ( int i = 0; i < dropStrings.length; i++ ) {
				dropStrings[i] = dropStrings[i].replaceFirst( "^(?i)drop index ", "drop vector index " );
			}
		}
		return dropStrings;
	}

	private String buildVectorIndexDdl(Index index, Metadata metadata, SqlStringGenerationContext context) {
		final String tableName;
		if ( context != null && index.getTable() != null && index.getTable().getQualifiedTableName() != null ) {
			tableName = context.format( index.getTable().getQualifiedTableName() );
		}
		else if ( index.getTable() != null && index.getTable().getName() != null ) {
			tableName = dialect.quote( index.getTable().getName() );
		}
		else {
			throw new IllegalArgumentException( "Index table must not be null for index: " + ( index != null ? index.getName() : "null" ) );
		}

		final StringBuilder ddl = new StringBuilder( "create vector index " )
				.append( getIndexName( index, context, metadata ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );

		boolean first = true;
		Column vectorCol = null;
		for ( Selectable selectable : index.getSelectables() ) {
			if ( !first ) {
				ddl.append( ", " );
			}
			first = false;
			ddl.append( selectable.getText( dialect ) );
			if ( selectable instanceof Column c ) {
				Integer code = c.getSqlTypeCode();
				if ( code == null && metadata != null ) {
					try {
						code = c.getSqlTypeCode( metadata );
					}
					catch (Exception ignored) {
					}
				}
				if ( code != null && isVectorSqlType( code ) ) {
					vectorCol = c;
				}
				else if ( vectorCol == null ) {
					vectorCol = c;
				}
			}
		}
		ddl.append( ")" );

		if ( vectorCol != null && vectorCol.isNullable() ) {
			ddl.append( " where " ).append( vectorCol.getText( dialect ) ).append( " is not null" );
		}

		final String options = index.getOptions();
		if ( isNotBlank( options ) ) {
			final String trimmed = options.trim();
			final String lower = trimmed.toLowerCase( Locale.ROOT );
			if ( lower.startsWith( "options" ) ) {
				ddl.append( " " ).append( trimmed );
			}
			else if ( lower.startsWith( "with" ) ) {
				ddl.append( " options " ).append( trimmed.substring( "with".length() ).trim() );
			}
			else if ( trimmed.startsWith( "(" ) ) {
				ddl.append( " options " ).append( trimmed );
			}
			else {
				ddl.append( " options (" ).append( trimmed ).append( ")" );
			}
		}
		else {
			ddl.append( " options (distance_type = 'COSINE')" );
		}
		return ddl.toString();
	}

	private String getIndexName(Index index, SqlStringGenerationContext context, Metadata metadata) {
		if ( dialect.getIndexDdlSupport().nameQualification() == IndexNameQualification.QUALIFIED ) {
			final var qualifiedTableName = index.getTable().getQualifiedTableName();
			if ( metadata != null && metadata.getDatabase() != null && metadata.getDatabase().getJdbcEnvironment() != null ) {
				return context.format(
						new QualifiedNameImpl(
								qualifiedTableName.getCatalogName(),
								qualifiedTableName.getSchemaName(),
								metadata.getDatabase().getJdbcEnvironment().getIdentifierHelper()
										.toIdentifier( index.getQuotedName( dialect ) )
						)
				);
			}
			else {
				return qualify( context.format( qualifiedTableName ), index.getName() );
			}
		}
		else {
			return index.getName();
		}
	}
}
