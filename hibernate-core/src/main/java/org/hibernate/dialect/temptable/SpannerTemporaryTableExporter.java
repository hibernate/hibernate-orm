/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.dialect.SpannerDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Spanner-specific exporter for temporary tables.
 * Spanner requires PRIMARY KEY definitions to be outside the column definition list.
 * * Syntax: CREATE TABLE Name (Col Type) PRIMARY KEY (Col)
 */
public class SpannerTemporaryTableExporter extends StandardTemporaryTableExporter {

	private final SpannerDialect dialect;

	public SpannerTemporaryTableExporter(SpannerDialect dialect) {
		super( dialect );
		this.dialect = dialect;
	}

	@Override
	public String getSqlCreateCommand(TemporaryTable temporaryTable) {
		final StringBuilder buffer = new StringBuilder( dialect.getCreateTableString() )
				.append( ' ' )
				.append( temporaryTable.getQualifiedTableName() )
				.append( " (" );
		final List<String> primaryKeyColumnNames = new ArrayList<>();
		boolean first = true;
		for ( TemporaryTableColumn column : temporaryTable.getColumnsForExport() ) {
			if ( first ) {
				first = false;
			}
			else {
				buffer.append( ", " );
			}
			buffer.append( column.getColumnName() ).append( ' ' );
			final String databaseTypeName = column.getSqlTypeDefinition();
			buffer.append( databaseTypeName );
			if ( !column.isNullable() ) {
				buffer.append( " not null" );
			}
			// Track PK
			if ( column.isPrimaryKey() ) {
				primaryKeyColumnNames.add( column.getColumnName() );
			}
		}
		buffer.append( ')' );
		// append primary key (outside the column declaration for Spanner)
		// primary key () is still needed even if there are no primary key columns
		buffer.append( " primary key (" );
		if ( !primaryKeyColumnNames.isEmpty() ) {
			buffer.append( String.join( ", ", primaryKeyColumnNames ) );
		}
		buffer.append( ')' );
		return buffer.toString();
	}

	@Override
	public String getSqlTruncateCommand(
			TemporaryTable temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		if ( temporaryTable.getSessionUidColumn() != null ) {
			return super.getSqlTruncateCommand( temporaryTable, sessionUidAccess, session );
		}
		else {
			return dialect.getTruncateTableStatement( temporaryTable.getQualifiedTableName() );
		}
	}

}
