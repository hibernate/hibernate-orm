/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.function.Function;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class PhysicalIdTableExporter implements IdTableExporter {

	protected String getCreateCommand() {
		return "create table";
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
			buffer.append( column.getSqlTypeDefinition() );
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
		return getDropCommand() + ' ' + idTable.getQualifiedTableName();
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
