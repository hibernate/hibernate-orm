/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.BooleanType;

class SDOBooleanType extends BooleanType {

	private static final long serialVersionUID = 1L;

	/**
	 * <p/>
	 * This type's name is <tt>sdo_boolean</tt>
	 */
	public String getName() {
		return "sdo_boolean";
	}

	public Object get(ResultSet rs, String name) throws SQLException {
		final String value = rs.getString( name );
		if ( rs.wasNull() ) {
			return getDefaultValue();
		}
		else if ( "TRUE".equalsIgnoreCase( value ) ) {
			return Boolean.TRUE;
		}
		else {
			return Boolean.FALSE;
		}
	}

	public void set(PreparedStatement st, Boolean value, int index)
			throws SQLException {

		if ( value == null ) {
			st.setNull( index, Types.VARCHAR );
		}
		else {
			st.setString( index, value ? "TRUE" : "FALSE" );
		}
	}

	public String objectToSQLString(Boolean value, Dialect dialect) {
		return value ? "'TRUE'" : "'FALSE'";
	}

}
