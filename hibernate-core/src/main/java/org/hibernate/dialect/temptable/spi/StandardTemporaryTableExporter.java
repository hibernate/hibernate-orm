/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import java.util.Locale;
import java.util.function.Function;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Standard composition-based temporary-table exporter.
///
/// @see Dialect#getTemporaryTableExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class StandardTemporaryTableExporter implements TemporaryTableExporter {
	private final Dialect dialect;

	public StandardTemporaryTableExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	private TemporaryTableStrategy getDefaultTemporaryTableStrategy(TemporaryTableDescriptor temporaryTable) {
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
	public String getSqlCreateCommand(TemporaryTableDescriptor temporaryTable) {
		final TemporaryTableStrategy temporaryTableStrategy = getDefaultTemporaryTableStrategy( temporaryTable );
		final var buffer = new StringBuilder( temporaryTableStrategy.getTemporaryTableCreateCommand() ).append( ' ' );
		buffer.append( temporaryTable.getQualifiedTableName() );
		buffer.append( '(' );

		for ( TemporaryTableColumnDescriptor column : temporaryTable.getColumnsForExport() ) {
			buffer.append( column.getColumnName() );
			final int sqlTypeCode = column.getJdbcMapping().getJdbcType().getDdlTypeCode();
			final String databaseTypeName = column.getSqlTypeDefinition();

			final String columnAnnotation = temporaryTableStrategy.getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
			final String annotatedType = columnAnnotation.isEmpty()
					? databaseTypeName
					: databaseTypeName + ' ' + columnAnnotation;
			if ( temporaryTableStrategy.supportsTemporaryTableNullConstraint()
					&& !databaseTypeName.toLowerCase( Locale.ROOT ).contains( "not null" ) ) {
				dialect.getColumnDefinitionSupport().appendDefinition(
						new StringBuilderSqlAppender( buffer ),
						new ColumnDefinitionRequest( annotatedType, null, column.isNullable(), null, null )
				);
			}
			else {
				buffer.append( ' ' ).append( annotatedType );
			}
			buffer.append( ", " );
		}
		if ( temporaryTableStrategy.supportsTemporaryTablePrimaryKey() ) {
			buffer.append( "primary key (" );
			for ( TemporaryTableColumnDescriptor column : temporaryTable.getColumnsForExport() ) {
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

		final String createOptions = temporaryTableStrategy.getTemporaryTableCreateOptions();
		if ( createOptions != null ) {
			buffer.append( ' ' ).append( createOptions );
		}

		return buffer.toString();
	}

	@Override
	public String getSqlDropCommand(TemporaryTableDescriptor temporaryTable) {
		final TemporaryTableStrategy temporaryTableStrategy = getDefaultTemporaryTableStrategy( temporaryTable );
		return temporaryTableStrategy.getTemporaryTableDropCommand() + " " + temporaryTable.getQualifiedTableName();
	}

	@Override
	public String getSqlTruncateCommand(
			TemporaryTableDescriptor temporaryTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		final TemporaryTableStrategy temporaryTableStrategy = getDefaultTemporaryTableStrategy( temporaryTable );
		if ( temporaryTable.getSessionUidColumn() != null ) {
			final ParameterMarkerStrategy parameterMarkerStrategy =
					session.getSessionFactory().getParameterMarkerStrategy();
			return temporaryTableStrategy.getTemporaryTableTruncateCommand() + " " + temporaryTable.getQualifiedTableName()
					+ " where " + temporaryTable.getSessionUidColumn().getColumnName() + " = "
					+ parameterMarkerStrategy.createMarker( 1, null );
		}
		else {
			return temporaryTableStrategy.getTemporaryTableTruncateCommand() + " " + temporaryTable.getQualifiedTableName();
		}
	}
}
