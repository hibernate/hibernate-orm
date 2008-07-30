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
 * <tt>double</tt>: A type that maps an SQL DOUBLE to a Java Double.
 * @author Gavin King
 */
public class DoubleType extends PrimitiveType {

	public Serializable getDefaultValue() {
		return new Double(0.0);
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return new Double( rs.getDouble(name) );
	}

	public Class getPrimitiveClass() {
		return double.class;
	}

	public Class getReturnedClass() {
		return Double.class;
	}

	public void set(PreparedStatement st, Object value, int index)
		throws SQLException {

		st.setDouble( index, ( (Double) value ).doubleValue() );
	}

	public int sqlType() {
		return Types.DOUBLE;
	}
	public String getName() { return "double"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return value.toString();
	}

	public Object fromStringValue(String xml) {
		return new Double(xml);
	}

}





