/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.type;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.hibernate.dialect.Dialect;

/**
 * Maps {@link Types#INTEGER interger} database values to boolean java values.  Zero is considered false;
 * <tt>NULL</tt> maps to {@link #getDefaultValue()}; any other value is considered true.
 *
 * @author Steve Ebersole
 * @see #getName()
 */
public class NumericBooleanType extends BooleanType {

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This type's name is <tt>numeric_boolean</tt>
	 */
	public String getName() {
		return "numeric_boolean";
	}

	/**
	 * {@inheritDoc}
	 */
	public Object get(ResultSet rs, String name) throws SQLException {
		int value = rs.getInt( name );
		if ( rs.wasNull() ) {
			return getDefaultValue();
		}
		else if ( value == 0 ) {
			return Boolean.FALSE;
		}
		else {
			return Boolean.TRUE;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.INTEGER );
		}
		else {
			boolean bool = ( ( Boolean ) value ).booleanValue();
			st.setInt( index, bool ? 1 : 0 );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return ( ( Boolean ) value ).booleanValue() ? "1" : "0";
	}

	/**
	 * {@inheritDoc}
	 */
	public int sqlType() {
		return Types.INTEGER;
	}
}
