/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * The exporter for Cloud Spanner CREATE and DROP table statements.
 *
 * @author Chengyuan Zhao
 * @author Daniel Zou
 */
public class SpannerDialectTableExporter implements Exporter<ExportableTable> {

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
	public String[] getSqlCreateStrings(ExportableTable table, JdbcServices jdbcServices) {

		Collection<PhysicalColumn> keyColumns;

		if ( table.hasPrimaryKey() ) {
			// a typical table that corresponds to an entity type
			keyColumns = table.getPrimaryKey().getColumns();
		}
		else if ( table.getForeignKeys().size() > 0 ) {
			// a table with no PK's but has FK's; often corresponds to element collection properties
			keyColumns = table.getPhysicalColumns();
		}
		else {
			// the case corresponding to a sequence-table that will only have 1 row.
			keyColumns = Collections.emptyList();
		}

		return getTableString( table, keyColumns );
	}

	private String[] getTableString(ExportableTable table, Iterable<PhysicalColumn> keyColumns) {
		String primaryKeyColNames = StreamSupport.stream( keyColumns.spliterator(), false )
				.map( col -> col.getName().render() )
				.collect( Collectors.joining( "," ) );

		StringJoiner colsAndTypes = new StringJoiner( "," );

		for ( PhysicalColumn col : table.getPhysicalColumns() ) {
			String columnDeclaration =
					col.getName().render()
							+ " " + col.getSqlTypeName()
							+ ( col.isNullable() ? this.spannerDialect.getNullColumnString() : " not null" );
			colsAndTypes.add( columnDeclaration );
		}

		ArrayList<String> statements = new ArrayList<>();
		statements.add(
				MessageFormat.format(
						this.createTableTemplate,
						table.getTableName().render(),
						colsAndTypes.toString(),
						primaryKeyColNames
				) );

		// Hibernate requires the special hibernate_sequence table to be populated with an initial val.
		if ( table.getTableName().getText().equals( SequenceStyleGenerator.DEF_SEQUENCE_NAME ) ) {
			statements.add( "INSERT INTO " + SequenceStyleGenerator.DEF_SEQUENCE_NAME + " ("
					+ SequenceStyleGenerator.DEF_VALUE_COLUMN + ") VALUES(1)" );
		}

		return statements.toArray( new String[0] );
	}

	@Override
	public String[] getSqlDropStrings(ExportableTable table, JdbcServices jdbcServices) {

		/* Cloud Spanner requires examining the metadata to find all indexes and interleaved tables.
		 * These must be dropped before the given table can be dropped.
		 * The current implementation does not support interleaved tables.
		 */

		ArrayList<String> dropStrings = new ArrayList<>();

		for ( Index index : table.getIndexes() ) {
			dropStrings.add( "drop index " + index.getName().render() );
		}

		dropStrings.add( this.spannerDialect.getDropTableString( table.getTableName().render() ) );

		return dropStrings.toArray( new String[0] );
	}
}
