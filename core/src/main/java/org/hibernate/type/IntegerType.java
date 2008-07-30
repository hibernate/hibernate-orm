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
import java.util.Comparator;

import org.hibernate.util.ComparableComparator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;

/**
 * <tt>integer</tt>: A type that maps an SQL INT to a Java Integer.
 * @author Gavin King
 */
public class IntegerType extends PrimitiveType implements DiscriminatorType, VersionType {

	private static final Integer ZERO = new Integer(0);

	public Serializable getDefaultValue() {
		return ZERO;
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return new Integer( rs.getInt(name) );
	}

	public Class getPrimitiveClass() {
		return int.class;
	}

	public Class getReturnedClass() {
		return Integer.class;
	}

	public void set(PreparedStatement st, Object value, int index)
	throws SQLException {
		st.setInt( index, ( (Integer) value ).intValue() );
	}

	public int sqlType() {
		return Types.INTEGER;
	}

	public String getName() { return "integer"; }

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return value.toString();
	}

	public Object stringToObject(String xml) throws Exception {
		return new Integer(xml);
	}

	public Object next(Object current, SessionImplementor session) {
		return new Integer( ( (Integer) current ).intValue() + 1 );
	}

	public Object seed(SessionImplementor session) {
		return ZERO;
	}

	public Comparator getComparator() {
		return ComparableComparator.INSTANCE;
	}
	
	public Object fromStringValue(String xml) {
		return new Integer(xml);
	}

}
