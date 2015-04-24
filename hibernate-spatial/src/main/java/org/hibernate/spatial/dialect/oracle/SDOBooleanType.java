/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
