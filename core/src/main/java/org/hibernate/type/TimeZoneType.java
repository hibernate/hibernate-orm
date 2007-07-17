//$Id: TimeZoneType.java 7825 2005-08-10 20:23:55Z oneovthafew $
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






