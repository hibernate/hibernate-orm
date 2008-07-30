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

import org.hibernate.dialect.Dialect;

/**
 * <tt>string</tt>: A type that maps an SQL VARCHAR to a Java String.
 * @author Gavin King
 */
public class StringType extends ImmutableType implements DiscriminatorType {

	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getString(name);
	}

	public Class getReturnedClass() {
		return String.class;
	}

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		st.setString(index, (String) value);
	}

	public int sqlType() {
		return Types.VARCHAR;
	}

	public String getName() { return "string"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return '\'' + (String) value + '\'';
	}

	public Object stringToObject(String xml) throws Exception {
		return xml;
	}

	public String toString(Object value) {
		return (String) value;
	}

	public Object fromStringValue(String xml) {
		return xml;
	}

}





