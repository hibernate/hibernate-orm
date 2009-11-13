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

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.SerializationHelper;

/**
 * <tt>serializable</tt>: A type that maps an SQL VARBINARY to a
 * serializable Java object.
 * @author Gavin King
 */
public class SerializableType extends MutableType {

	private final Class serializableClass;

	public SerializableType(Class serializableClass) {
		this.serializableClass = serializableClass;
	}

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		Hibernate.BINARY.set(st, toBytes(value), index);
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		byte[] bytes = (byte[]) Hibernate.BINARY.get(rs, name);
		// Some JDBC drivers erroneously return an empty array here for a null DB value :/
		if ( bytes == null || bytes.length == 0 ) {
			return null;
		}
		else {
			return fromBytes(bytes);
		}
	}

	public Class getReturnedClass() {
		return serializableClass;
	}

	public boolean isEqual(Object x, Object y) throws HibernateException {
		if ( x == y ) {
			return true;
		}
		if ( x == null || y == null ) {
			return false;
		}
		return x.equals( y ) || Hibernate.BINARY.isEqual( toBytes( x ), toBytes( y ) );
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		return Hibernate.BINARY.getHashCode( toBytes(x), entityMode );
	}

	public String toString(Object value) throws HibernateException {
		return Hibernate.BINARY.toString( toBytes(value) );
	}

	public Object fromStringValue(String xml) throws HibernateException {
		return fromBytes( (byte[]) Hibernate.BINARY.fromStringValue(xml) );
	}

	public String getName() {
		return (serializableClass==Serializable.class) ? "serializable" : serializableClass.getName();
	}

	public Object deepCopyNotNull(Object value) throws HibernateException {
		return fromBytes( toBytes(value) );
	}

	private static byte[] toBytes(Object object) throws SerializationException {
		return SerializationHelper.serialize( (Serializable) object );
	}

	private Object fromBytes(byte[] bytes) throws SerializationException {
		return SerializationHelper.deserialize( bytes, getReturnedClass().getClassLoader() );
	}

	public int sqlType() {
		return Hibernate.BINARY.sqlType();
	}

	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
	throws HibernateException {
		return (cached==null) ? null : fromBytes( (byte[]) cached );
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		return (value==null) ? null : toBytes(value);
	}

}







