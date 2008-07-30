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

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;

/**
 * <tt>character</tt>: A type that maps an SQL CHAR(1) to a Java Character.
 * @author Gavin King
 */
public class CharacterType extends PrimitiveType implements DiscriminatorType {

	public Serializable getDefaultValue() {
		throw new UnsupportedOperationException("not a valid id type");
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		String str = rs.getString(name);
		if (str==null) {
			return null;
		}
		else {
			return new Character( str.charAt(0) );
		}
	}

	public Class getPrimitiveClass() {
		return char.class;
	}

	public Class getReturnedClass() {
		return Character.class;
	}

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		st.setString( index, (value).toString() );
	}

	public int sqlType() {
		return Types.CHAR;
	}
	public String getName() { return "character"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return '\'' + value.toString() + '\'';
	}

	public Object stringToObject(String xml) throws Exception {
		if ( xml.length() != 1 ) throw new MappingException("multiple or zero characters found parsing string");
		return new Character( xml.charAt(0) );
	}

	public Object fromStringValue(String xml) {
		return new Character( xml.charAt(0) );
	}

}





