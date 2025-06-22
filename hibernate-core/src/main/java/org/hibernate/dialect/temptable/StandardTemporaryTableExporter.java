/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * @author Steve Ebersole
 */
public class StandardTemporaryTableExporter implements TemporaryTableExporter {
	private final Dialect dialect;

	public StandardTemporaryTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	protected String getCreateCommand() {
		return dialect.getTemporaryTableCreateCommand();
	}

	protected String getCreateOptions() {
		return dialect.getTemporaryTableCreateOptions();
	}

	protected String getDropCommand() {
		return dialect.getTemporaryTableDropCommand();
	}

	protected String getTruncateTableCommand() {
		return dialect.getTemporaryTableTruncateCommand();
	}

	@Override
	public String getSqlCreateCommand(TemporaryTable temporaryTable) {
		final StringBuilder buffer = new StringBuilder( getCreateCommand() ).append( ' ' );
		buffer.append( temporaryTable.getQualifiedTableName() );
		buffer.append( '(' );

		for ( TemporaryTableColumn column : temporaryTable.getColumnsForExport() ) {
			buffer.append( column.getColumnName() ).append( ' ' );
			final int sqlTypeCode = column.getJdbcMapping().getJdbcType().getDdlTypeCode();
			final String databaseTypeName = column.getSqlTypeDefinition();

			buffer.append( databaseTypeName );

			final String columnAnnotation = dialect.getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
			if ( !columnAnnotation.isEmpty() ) {
				buffer.append( ' ' ).append( columnAnnotation );
			}

			if ( column.isNullable() ) {
				final String nullColumnString = dialect.getNullColumnString( databaseTypeName );
				if ( !databaseTypeName.contains( nullColumnString ) ) {
					buffer.append( nullColumnString );
				}
			}
			else {
				buffer.append( " not null" );
			}
			buffer.append( ", " );
		}
		if ( dialect.supportsTemporaryTablePrimaryKey() ) {
			buffer.append( "primary key (" );
			for ( TemporaryTableColumn column : temporaryTable.getColumnsForExport() ) {
				if ( column.isPrimaryKey() ) {
					buffer.append( column.getColumnName() );
					buffer.append( ", " );
				}
			}
			buffer.setLength( buffer.length() - 2 );
			buffer.append( ')' );
		}
		else {
			buffer.setLength( buffer.length() - 2 );
		}
		buffer.append( ')' );

		final String createOptions = getCreateOptions();
		if ( createOptions != null ) {
			buffer.append( ' ' ).append( createOptions );
		}

		return buffer.toString();
	}

	@Override
	public String getSqlDropCommand(TemporaryTable idTable) {
		return getDropCommand() + " " + idTable.getQualifiedTableName();
	}

	@Override
	public String getSqlTruncateCommand(
			TemporaryTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		if ( idTable.getSessionUidColumn() != null ) {
			final ParameterMarkerStrategy parameterMarkerStrategy =
					session.getSessionFactory().getParameterMarkerStrategy();
			return getTruncateTableCommand() + " " + idTable.getQualifiedTableName()
					+ " where " + idTable.getSessionUidColumn().getColumnName() + " = "
					+ parameterMarkerStrategy.createMarker( 1, null );
		}
		else {
			return getTruncateTableCommand() + " " + idTable.getQualifiedTableName();
		}
	}
}
