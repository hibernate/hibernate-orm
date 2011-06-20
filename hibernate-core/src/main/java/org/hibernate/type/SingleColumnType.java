/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Provide convenient methods for binding and extracting values for use with {@link BasicType}.  Most of this
 * is copied from the (now deprecated) {@link NullableType}.
 * <p/>
 * Glaring omission are the forms that do not take
 *
 * @author Steve Ebersole
 */
public interface SingleColumnType<T> extends Type {

	public int sqlType();

	public String toString(T value) throws HibernateException;

	public T fromStringValue(String xml) throws HibernateException;

	/**
	 * Get a column value from a result set by name.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param name The name of the value to extract.
	 * @param session The session from which the request originates
	 *
	 * @return The extracted value.
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public T nullSafeGet(ResultSet rs, String name, SessionImplementor session) throws HibernateException, SQLException;

	/**
	 * Get a column value from a result set, without worrying about the possibility of null values.
	 *
	 * @param rs The result set from which to extract the value.
	 * @param name The name of the value to extract.
	 * @param session The session from which the request originates
	 *
	 * @return The extracted value.
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public Object get(ResultSet rs, String name, SessionImplementor session) throws HibernateException, SQLException;

	/**
	 * Set a parameter value without worrying about the possibility of null
	 * values.  Called from {@link #nullSafeSet} after nullness checks have
	 * been performed.
	 *
	 * @param st The statement into which to bind the parameter value.
	 * @param value The parameter value to bind.
	 * @param index The position or index at which to bind the param value.
	 * @param session The session from which the request originates
	 *
	 * @throws org.hibernate.HibernateException Generally some form of mismatch error.
	 * @throws java.sql.SQLException Indicates problem making the JDBC call(s).
	 */
	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws HibernateException, SQLException;
}
