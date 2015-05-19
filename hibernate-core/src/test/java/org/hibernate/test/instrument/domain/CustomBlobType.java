/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.domain;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.UserType;

/**
 * A simple byte[]-based custom type.
 */
public class CustomBlobType implements UserType {
	/**
	 * {@inheritDoc}
	 */
	public Object nullSafeGet(ResultSet rs, String names[], SessionImplementor session, Object owner) throws SQLException {
		// cast just to make sure...
		return StandardBasicTypes.BINARY.nullSafeGet( rs, names[0], session );
	}

	/**
	 * {@inheritDoc}
	 */
	public void nullSafeSet(PreparedStatement ps, Object value, int index, SessionImplementor session) throws SQLException, HibernateException {
		// cast just to make sure...
		StandardBasicTypes.BINARY.nullSafeSet( ps, value, index, session );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object deepCopy(Object value) {
		byte result[] = null;

		if ( value != null ) {
			byte bytes[] = ( byte[] ) value;

			result = new byte[bytes.length];
			System.arraycopy( bytes, 0, result, 0, bytes.length );
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isMutable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public int[] sqlTypes() {
		return new int[] { Types.VARBINARY };
	}

	/**
	 * {@inheritDoc}
	 */
	public Class returnedClass() {
		return byte[].class;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object x, Object y) {
		return Arrays.equals( ( byte[] ) x, ( byte[] ) y );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object assemble(Serializable arg0, Object arg1)
			throws HibernateException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Serializable disassemble(Object arg0)
			throws HibernateException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode(Object arg0)
			throws HibernateException {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object replace(Object arg0, Object arg1, Object arg2)
			throws HibernateException {
		return null;
	}
}
