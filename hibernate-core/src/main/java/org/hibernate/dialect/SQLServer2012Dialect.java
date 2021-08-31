/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQLServer2012LimitHandler;

/**
 * Microsoft SQL Server 2012 Dialect
 *
 * @author Brett Meyer
 */
public class SQLServer2012Dialect extends SQLServer2008Dialect {

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getQuerySequencesString() {
		// The upper-case name is necessary here so that both case-sensitive and case-insensitive collations work

		// Internally, SQL server stores start_value, minimum_value, maximum_value, and increment
		// in sql_variant columns. SQL Server's JDBC automatically converts these values
		// to bigint. Vert.X does support sql_variant columns, so these columns need to be
		// explicitly converted here.

		return "select sequence_name, sequence_catalog, sequence_schema, " +
				"convert( bigint, start_value ) as start_value, " +
				"convert( bigint, minimum_value ) as minimum_value, " +
				"convert( bigint, maximum_value ) as maximum_value, " +
				"convert( bigint, increment ) as increment " +
				"from INFORMATION_SCHEMA.SEQUENCES";
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		final StringBuilder buffer = new StringBuilder(
				sql.length()
						+ hints.length() + 12
		);
		final int pos = sql.indexOf( ';' );
		if ( pos > -1 ) {
			buffer.append( sql.substring( 0, pos ) );
		}
		else {
			buffer.append( sql );
		}
		buffer.append( " OPTION (" ).append( hints ).append( ")" );
		if ( pos > -1 ) {
			buffer.append( ";" );
		}
		sql = buffer.toString();

		return sql;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	protected LimitHandler getDefaultLimitHandler() {
		return new SQLServer2012LimitHandler();
	}
}
