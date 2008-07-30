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
import java.util.TimeZone;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * <tt>timezone</tt>: A type that maps an SQL VARCHAR to a
 * <tt>java.util.TimeZone</tt>
 * @see java.util.TimeZone
 * @author Gavin King
 */
public class TimeZoneType extends ImmutableType implements LiteralType {

	public Object get(ResultSet rs, String name)
	throws HibernateException, SQLException {
		String id = (String) Hibernate.STRING.nullSafeGet(rs, name);
		return (id==null) ? null : TimeZone.getTimeZone(id);
	}


	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		Hibernate.STRING.set(st, ( (TimeZone) value ).getID(), index);
	}

	public int sqlType() {
		return Hibernate.STRING.sqlType();
	}

	public String toString(Object value) throws HibernateException {
		return ( (TimeZone) value ).getID();
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		return ( (TimeZone) x ).getID().compareTo( ( (TimeZone) y ).getID() );
	}

	public Object fromStringValue(String xml) throws HibernateException {
		return TimeZone.getTimeZone(xml);
	}

	public Class getReturnedClass() {
		return TimeZone.class;
	}

	public String getName() {
		return "timezone";
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return ( (LiteralType) Hibernate.STRING ).objectToSQLString(
			( (TimeZone) value ).getID(), dialect
		);
	}

}






