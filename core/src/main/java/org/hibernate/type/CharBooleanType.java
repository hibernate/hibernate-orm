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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;


/**
 * Superclass for types that map Java boolean to SQL CHAR(1).
 * @author Gavin King
 */
public abstract class CharBooleanType extends BooleanType {

	protected abstract String getTrueString();
	protected abstract String getFalseString();

	public Object get(ResultSet rs, String name) throws SQLException {
		String code = rs.getString(name);
		if ( code==null || code.length()==0 ) {
			return null;
		}
		else {
			return getTrueString().equalsIgnoreCase( code.trim() ) ? 
					Boolean.TRUE : Boolean.FALSE;
		}
	}

	public void set(PreparedStatement st, Object value, int index)
	throws SQLException {
		st.setString( index, toCharacter(value) );

	}

	public int sqlType() {
		return Types.CHAR;
	}

	private String toCharacter(Object value) {
		return ( (Boolean) value ).booleanValue() ? getTrueString() : getFalseString();
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return "'" + toCharacter(value) + "'";
	}

	public Object stringToObject(String xml) throws Exception {
		if ( getTrueString().equalsIgnoreCase(xml) ) {
			return Boolean.TRUE;
		}
		else if ( getFalseString().equalsIgnoreCase(xml) ) {
			return Boolean.FALSE;
		}
		else {
			throw new HibernateException("Could not interpret: " + xml);
		}
	}

}







