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

	@Deprecated(forRemoval = true, since = "7.1")
	protected String getCreateCommand() {
		return dialect.getTemporaryTableCreateCommand();
	}

	protected String getCreateCommand(TemporaryTableStrategy temporaryTableStrategy) {
		return temporaryTableStrategy.getTemporaryTableCreateCommand();
	}

	@Deprecated(forRemoval = true, since = "7.1")
	protected String getCreateOptions() {
		return dialect.getTemporaryTableCreateOptions();
	}

	protected String getCreateOptions(TemporaryTableStrategy temporaryTableStrategy) {
		return temporaryTableStrategy.getTemporaryTableCreateOptions();
	}

	@Deprecated(forRemoval = true, since = "7.1")
	protected String getDropCommand() {
		return dialect.getTemporaryTableDropCommand();
	}

	protected String getDropCommand(TemporaryTableStrategy temporaryTableStrategy) {
		return temporaryTableStrategy.getTemporaryTableDropCommand();
	}

	@Deprecated(forRemoval = true, since = "7.1")
	protected String getTruncateTableCommand() {
		return dialect.getTemporaryTableTruncateCommand();
	}

	protected String getTruncateTableCommand(TemporaryTableStrategy temporaryTableStrategy) {
		return temporaryTableStrategy.getTemporaryTableTruncateCommand();
	}

	private TemporaryTableStrategy getDefaultTemporaryTableStrategy(TemporaryTable temporaryTable) {
		final TemporaryTableStrategy temporaryTableStrategy = switch ( temporaryTable.getTemporaryTableKind() ) {
					case LOCAL -> dialect.getLocalTemporaryTableStrategy();
					case GLOBAL -> dialect.getGlobalTemporaryTableStrategy();
					case PERSISTENT -> dialect.getPersistentTemporaryTableStrategy();
				};
		if ( temporaryTableStrategy == null ) {
			throw new IllegalStateException(
					"Dialect returns null TemporaryTableStrategy for temporary table " + temporaryTable.getQualifiedTableName() + " of type " + temporaryTable.getTemporaryTableKind() );
		}
		return temporaryTableStrategy;
	}

	@Override
	public String getSqlCreateCommand(TemporaryTable temporaryTable) {
		final TemporaryTableStrategy temporaryTableStrategy = getDefaultTemporaryTableStrategy( temporaryTable );
		final StringBuilder buffer = new StringBuilder( getCreateCommand( temporaryTableStrategy ) ).append( ' ' );
		buffer.append( temporaryTable.getQualifiedTableName() );
		buffer.append( '(' );

		for ( TemporaryTableColumn column : temporaryTable.getColumnsForExport() ) {
			buffer.append( column.getColumnName() ).append( ' ' );
			final int sqlTypeCode = column.getJdbcMapping().getJdbcType().getDdlTypeCode();
			final String databaseTypeName = column.getSqlTypeDefinition();

			buffer.append( databaseTypeName );

			final String columnAnnotation = temporaryTableStrategy.getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
			if ( !columnAnnotation.isEmpty() ) {
				buffer.append( ' ' ).append( columnAnnotation );
			}

			if ( temporaryTableStrategy.supportsTemporaryTableNullConstraint() ) {
				if ( column.isNullable() ) {
					final String nullColumnString = dialect.getNullColumnString( databaseTypeName );
					if ( !databaseTypeName.contains( nullColumnString ) ) {
						buffer.append( nullColumnString );
					}
				}
				else {
					buffer.append( " not null" );
				}
			}
			buffer.append( ", " );
		}
		if ( temporaryTableStrategy.supportsTemporaryTablePrimaryKey() ) {
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

		final String createOptions = getCreateOptions( temporaryTableStrategy );
		if ( createOptions != null ) {
			buffer.append( ' ' ).append( createOptions );
		}

		return buffer.toString();
	}

	@Override
	public String getSqlDropCommand(TemporaryTable temporaryTable) {
		final TemporaryTableStrategy temporaryTableStrategy = getDefaultTemporaryTableStrategy( temporaryTable );
		return getDropCommand( temporaryTableStrategy ) + " " + temporaryTable.getQualifiedTableName();
	}

	@Override
	public String getSqlTruncateCommand(
			TemporaryTable temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		final TemporaryTableStrategy temporaryTableStrategy = getDefaultTemporaryTableStrategy( temporaryTable );
		if ( temporaryTable.getSessionUidColumn() != null ) {
			final ParameterMarkerStrategy parameterMarkerStrategy =
					session.getSessionFactory().getParameterMarkerStrategy();
			return getTruncateTableCommand( temporaryTableStrategy ) + " " + temporaryTable.getQualifiedTableName()
					+ " where " + temporaryTable.getSessionUidColumn().getColumnName() + " = "
					+ parameterMarkerStrategy.createMarker( 1, null );
		}
		else {
			return getTruncateTableCommand( temporaryTableStrategy ) + " " + temporaryTable.getQualifiedTableName();
		}
	}
}
