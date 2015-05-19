/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.List;

import org.hibernate.internal.util.StringHelper;

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
		return "select name from sys.sequences";
	}

	@Override
	public String getQueryHintString(String sql, List<String> hints) {
		final String hint = StringHelper.join( ", ", hints.iterator() );

		if ( StringHelper.isEmpty( hint ) ) {
			return sql;
		}

		final StringBuilder buffer = new StringBuilder(
				sql.length()
						+ hint.length() + 12
		);
		final int pos = sql.indexOf( ";" );
		if ( pos > -1 ) {
			buffer.append( sql.substring( 0, pos ) );
		}
		else {
			buffer.append( sql );
		}
		buffer.append( " OPTION (" ).append( hint ).append( ")" );
		if ( pos > -1 ) {
			buffer.append( ";" );
		}
		sql = buffer.toString();

		return sql;
	}
}
