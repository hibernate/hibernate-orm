/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.function.Function;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class TempIdTableExporter implements IdTableExporter {
	private final boolean isLocal;
	private final Function<Integer, String> databaseTypeNameResolver;

	public TempIdTableExporter(
			boolean isLocal,
			Function<Integer, String> databaseTypeNameResolver) {
		this.isLocal = isLocal;
		this.databaseTypeNameResolver = databaseTypeNameResolver;
	}

	protected String getCreateCommand() {
		return "create " + (isLocal ? "local" : "global") + " temporary table";
	}

	protected String getCreateOptions() {
		return null;
	}

	protected String getDropCommand() {
		return "drop table";
	}

	protected String getTruncateIdTableCommand(){
		return "delete from";
	}

	@Override
	public String getSqlCreateCommand(IdTable idTable) {
		final StringBuilder buffer = new StringBuilder( getCreateCommand() ).append( ' ' );
		buffer.append( idTable.getQualifiedTableName() );
		buffer.append( '(' );

		boolean firstPass = true;
		for ( IdTableColumn column : idTable.getIdTableColumns() ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				buffer.append( ", " );
			}

			buffer.append( column.getColumnName() ).append( ' ' );
			final int sqlTypeCode = column.getJdbcMapping().getJdbcTypeDescriptor().getDefaultSqlTypeCode();
			final String databaseTypeName = column.getSqlTypeDefinition();

			buffer.append( " " ).append( databaseTypeName ).append( " " );

			final String columnAnnotation = idTable.getDialect().getCreateTemporaryTableColumnAnnotation( sqlTypeCode );
			if ( !columnAnnotation.isEmpty() ) {
				buffer.append(" ").append( columnAnnotation );
			}

			// id values cannot be null
			buffer.append( " not null" );
		}

		buffer.append( ") " );

		final String createOptions = getCreateOptions();
		if ( createOptions != null ) {
			buffer.append( createOptions );
		}

		return buffer.toString();
	}

	@Override
	public String getSqlDropCommand(IdTable idTable) {
		return getDropCommand() + " " + idTable.getQualifiedTableName();
	}

	@Override
	public String getSqlTruncateCommand(
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session) {
		if ( idTable.getSessionUidColumn() != null ) {
			assert sessionUidAccess != null;
			final String uid = sessionUidAccess.apply( session );
			return getTruncateIdTableCommand() + " " + idTable.getQualifiedTableName()
					+ " where " + idTable.getSessionUidColumn().getColumnName() + " = " + uid;
		}
		else {
			return getTruncateIdTableCommand() + " " + idTable.getQualifiedTableName();
		}
	}
}
