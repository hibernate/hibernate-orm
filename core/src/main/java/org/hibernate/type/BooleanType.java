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

import org.hibernate.dialect.Dialect;

/**
 * <tt>boolean</tt>: A type that maps an SQL BIT to a Java Boolean.
 * @author Gavin King
 */
public class BooleanType extends PrimitiveType implements DiscriminatorType {

	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getBoolean(name) ? Boolean.TRUE : Boolean.FALSE;
	}

	public Class getPrimitiveClass() {
		return boolean.class;
	}

	public Class getReturnedClass() {
		return Boolean.class;
	}

	public void set(PreparedStatement st, Object value, int index)
	throws SQLException {
		st.setBoolean( index, ( (Boolean) value ).booleanValue() );
	}

	public int sqlType() {
		return Types.BIT;
	}

	public String getName() { return "boolean"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return dialect.toBooleanValueString( ( (Boolean) value ).booleanValue() );
	}

	public Object stringToObject(String xml) throws Exception {
		return fromStringValue(xml);
	}

	public Object fromStringValue(String xml) {
		return Boolean.valueOf(xml);
	}

}





