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

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * <tt>currency</tt>: A type that maps an SQL VARCHAR to a
 * <tt>java.util.Currency</tt>
 * @see java.util.Currency
 * @author Gavin King
 */
public class CurrencyType extends ImmutableType implements LiteralType {

	public static final Class CURRENCY_CLASS;
	private static final Method CURRENCY_GET_INSTANCE;
	private static final Method CURRENCY_GET_CODE;

	static {
		Class clazz;
		try {
			clazz = Class.forName("java.util.Currency");
		}
		catch (ClassNotFoundException cnfe) {
			clazz = null;
		}
		if (clazz==null) {
			CURRENCY_CLASS = null;
			CURRENCY_GET_INSTANCE = null;
			CURRENCY_GET_CODE = null;
		}
		else {
			CURRENCY_CLASS = clazz;
			try {
				CURRENCY_GET_INSTANCE = clazz.getMethod("getInstance", new Class[] { String.class } );
				CURRENCY_GET_CODE = clazz.getMethod("getCurrencyCode", new Class[0] );
			}
			catch (Exception e) {
				throw new AssertionFailure("Exception in static initializer of CurrencyType", e);
			}
		}
	}

	/**
	 * @see org.hibernate.type.NullableType#get(ResultSet, String)
	 */
	public Object get(ResultSet rs, String name)
	throws HibernateException, SQLException {
		String code = (String) Hibernate.STRING.nullSafeGet(rs, name);
		try {
			return code==null ? null : 
					CURRENCY_GET_INSTANCE.invoke(null, new Object[] { code } );
		}
		catch (Exception e) {
			throw new HibernateException("Could not resolve currency code: " + code);
		}
	}

	/**
	 * @see org.hibernate.type.NullableType#set(PreparedStatement, Object, int)
	 */
	public void set(PreparedStatement st, Object value, int index)
	throws HibernateException, SQLException {
		Object code;
		try {
			code = CURRENCY_GET_CODE.invoke(value, null);
		}
		catch (Exception e) {
			throw new HibernateException("Could not get Currency code", e);
		}
		Hibernate.STRING.set(st, code, index);
	}

	/**
	 * @see org.hibernate.type.NullableType#sqlType()
	 */
	public int sqlType() {
		return Hibernate.STRING.sqlType();
	}

	/**
	 */
	public String toString(Object value) throws HibernateException {
		try {
			return (String) CURRENCY_GET_CODE.invoke(value, null);
		}
		catch (Exception e) {
			throw new HibernateException("Could not get Currency code", e);
		}
	}

	/**
	 * @see org.hibernate.type.Type#getReturnedClass()
	 */
	public Class getReturnedClass() {
		return CURRENCY_CLASS;
	}

	/**
	 * @see org.hibernate.type.Type#getName()
	 */
	public String getName() {
		return "currency";
	}

	/**
	 * @see org.hibernate.type.LiteralType#objectToSQLString(Object, Dialect)
	 */
	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		String code;
		try {
			code = (String) CURRENCY_GET_CODE.invoke(value, null);
		}
		catch (Exception e) {
			throw new HibernateException("Could not get Currency code", e);
		}
		return ( (LiteralType) Hibernate.STRING ).objectToSQLString(code, dialect);
	}

	public Object fromStringValue(String xml) throws HibernateException {
		try {
			return CURRENCY_GET_INSTANCE.invoke( null, new Object[] {xml} );
		}
		catch (Exception e) {
			throw new HibernateException("Could not resolve currency code: " + xml);
		}
	}

}






