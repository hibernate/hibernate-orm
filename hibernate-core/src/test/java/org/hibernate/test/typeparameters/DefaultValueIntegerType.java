/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.typeparameters;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;


/**
 * @author Michi
 */
public class DefaultValueIntegerType implements UserType, ParameterizedType, Serializable {
	private static final Logger log = Logger.getLogger( DefaultValueIntegerType.class );

	private Integer defaultValue;

	public int[] sqlTypes() {
		return new int[] {Types.INTEGER};
	}

	public Class returnedClass() {
		return int.class;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		if (x==y) return true;
		if (x==null || y==null) return false;
		return x.equals(y);
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		Number result = (Number) rs.getObject(names[0]);
		return result==null ? defaultValue : new Integer(result.intValue());
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if (value == null || defaultValue.equals(value) ) {
            log.trace("binding null to parameter: " + index);
			st.setNull(index, Types.INTEGER);
		} else {
            log.trace("binding " + value + " to parameter: " + index);
			st.setInt(index, ((Integer)value).intValue());
		}
	}

	public Object deepCopy(Object value) throws HibernateException {
		return new Integer(((Integer)value).intValue());
	}

	public boolean isMutable() {
		return false;
	}

	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	public Object assemble(Serializable cached, Object owner)
	throws HibernateException {
		return cached;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public Object replace(Object original, Object target, Object owner)
	throws HibernateException {
		return original;
	}

	public void setParameterValues(Properties parameters) {
		this.defaultValue = Integer.valueOf((String) parameters.get("default"));
	}

}
