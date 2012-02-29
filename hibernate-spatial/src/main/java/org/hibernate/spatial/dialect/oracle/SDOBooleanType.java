/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
		String value = rs.getString( name );
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
			boolean bool = value.booleanValue();
			st.setString( index, bool ? "TRUE" : "FALSE" );
		}
	}

	public String objectToSQLString(Boolean value, Dialect dialect) {
		return value.booleanValue() ? "'TRUE'" : "'FALSE'";
	}

//    public int sqlType() {
//        return Types.VARCHAR;
//    }

}