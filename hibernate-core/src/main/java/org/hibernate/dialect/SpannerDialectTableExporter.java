/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Exporter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
class SpannerDialectTableExporter implements Exporter<Table> {

	private final SpannerDialect spannerDialect;

	private final String createTableTemplate;

	/**
	 * Constructor.
	 *
	 * @param spannerDialect a Cloud Spanner dialect.
	 */
	public SpannerDialectTableExporter(SpannerDialect spannerDialect) {
		this.spannerDialect = spannerDialect;
		this.createTableTemplate =
				this.spannerDialect.getCreateTableString() + " {0} ({1}) PRIMARY KEY ({2})";
	}

	@Override
	public String[] getSqlCreateStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {

		Collection<Column> keyColumns;

		if ( table.hasPrimaryKey() ) {
			// a typical table that corresponds to an entity type
			keyColumns = table.getPrimaryKey().getColumns();
		}
		else if ( table.getForeignKeys().size() > 0 ) {
			// a table with no PK's but has FK's; often corresponds to element collection properties
			keyColumns = new ArrayList<>();
			for (Iterator<Column> column = table.getColumnIterator(); column.hasNext();) {
				keyColumns.add( column.next() );
			}
		}
		else {
			// the case corresponding to a sequence-table that will only have 1 row.
			keyColumns = Collections.emptyList();
		}

		return getTableString( table, keyColumns, context );
	}

	private String[] getTableString(Table table, Iterable<Column> keyColumns, SqlStringGenerationContext context) {
		String primaryKeyColNames = StreamSupport.stream( keyColumns.spliterator(), false )
				.map( Column::getName )
				.collect( Collectors.joining( "," ) );

		StringJoiner colsAndTypes = new StringJoiner( "," );


		for (Iterator<Column> column = table.getColumnIterator(); column.hasNext();) {
			Column col = column.next();
			String columnDeclaration =
					col.getName()
							+ " " + col.getSqlType()
							+ ( col.isNullable() ? this.spannerDialect.getNullColumnString( col.getSqlType() ) : " not null" );
			colsAndTypes.add( columnDeclaration );
		}

		ArrayList<String> statements = new ArrayList<>();
		statements.add(
				MessageFormat.format(
						this.createTableTemplate,
						context.format( table.getQualifiedTableName() ),
						colsAndTypes.toString(),
						primaryKeyColNames
				) );

		// Hibernate requires the special hibernate_sequence table to be populated with an initial val.
		if ( table.getName().equals( SequenceStyleGenerator.DEF_SEQUENCE_NAME ) ) {
			statements.add( "INSERT INTO " + SequenceStyleGenerator.DEF_SEQUENCE_NAME + " ("
					+ SequenceStyleGenerator.DEF_VALUE_COLUMN + ") VALUES(1)" );
		}

		return statements.toArray( new String[0] );
	}

	@Override
	public String[] getSqlDropStrings(Table table, Metadata metadata, SqlStringGenerationContext context) {

		/* Cloud Spanner requires examining the metadata to find all indexes and interleaved tables.
		 * These must be dropped before the given table can be dropped.
		 * The current implementation does not support interleaved tables.
		 */

		ArrayList<String> dropStrings = new ArrayList<>();

		for (Iterator<Index> index = table.getIndexIterator(); index.hasNext();) {
			dropStrings.add( "drop index " + index.next().getName() );
		}

		dropStrings.add( this.spannerDialect.getDropTableString( context.format( table.getQualifiedTableName() ) ) );

		return dropStrings.toArray( new String[0] );
	}
}
