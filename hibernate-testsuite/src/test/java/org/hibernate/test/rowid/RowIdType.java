//$Id: RowIdType.java 6477 2005-04-21 07:39:21Z oneovthafew $
package org.hibernate.test.rowid;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * @author Gavin King
 */
public class RowIdType implements UserType {

	public int[] sqlTypes() {
		return new int[] { Types.JAVA_OBJECT };
	}

	public Class returnedClass() {
		return Object.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		return x.equals(y);
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException {
		return rs.getObject( names[0] );
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {
		throw new UnsupportedOperationException();
	}

	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return null;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return null;
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return null;
	}

}
