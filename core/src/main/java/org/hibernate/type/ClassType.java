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

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;

/**
 * <tt>class</tt>: A type that maps an SQL VARCHAR to a Java Class.
 * @author Gavin King
 */
public class ClassType extends ImmutableType {

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		String str = (String) Hibernate.STRING.get(rs, name);
		if (str == null) {
			return null;
		}
		else {
			try {
				return ReflectHelper.classForName(str);
			}
			catch (ClassNotFoundException cnfe) {
				throw new HibernateException("Class not found: " + str);
			}
		}
	}

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		//TODO: would be nice to handle proxy classes elegantly!
		Hibernate.STRING.set(st, ( (Class) value ).getName(), index);
	}

	public int sqlType() {
		return Hibernate.STRING.sqlType();
	}

	public String toString(Object value) throws HibernateException {
		return ( (Class) value ).getName();
	}

	public Class getReturnedClass() {
		return Class.class;
	}

	public String getName() {
		return "class";
	}

	public Object fromStringValue(String xml) throws HibernateException {
		try {
			return ReflectHelper.classForName(xml);
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException("could not parse xml", cnfe);
		}
	}

}






