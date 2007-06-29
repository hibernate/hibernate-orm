//$Id: TimeType.java 8891 2005-12-21 05:13:29Z oneovthafew $
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * <tt>time</tt>: A type that maps an SQL TIME to a Java
 * java.util.Date or java.sql.Time.
 * @author Gavin King
 */
public class TimeType extends MutableType implements LiteralType {

	private static final String TIME_FORMAT = "HH:mm:ss";

	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getTime(name);
	}
	public Class getReturnedClass() {
		return java.util.Date.class;
	}
	public void set(PreparedStatement st, Object value, int index) throws SQLException {

		Time time;
		if (value instanceof Time) {
			time = (Time) value;
		}
		else {
			time = new Time( ( (java.util.Date) value ).getTime() );
		}
		st.setTime(index, time);
	}

	public int sqlType() {
		return Types.TIME;
	}
	public String getName() { return "time"; }

	public String toString(Object val) {
		return new SimpleDateFormat(TIME_FORMAT).format( (java.util.Date) val );
	}
	public boolean isEqual(Object x, Object y) {

		if (x==y) return true;
		if (x==null || y==null) return false;

		Date xdate = (Date) x;
		Date ydate = (Date) y;
		
		if ( xdate.getTime()==ydate.getTime() ) return true;
		
		Calendar calendar1 = java.util.Calendar.getInstance();
		Calendar calendar2 = java.util.Calendar.getInstance();
		calendar1.setTime( xdate );
		calendar2.setTime( ydate );

		return calendar1.get(Calendar.HOUR_OF_DAY) == calendar2.get(Calendar.HOUR_OF_DAY)
			&& calendar1.get(Calendar.MINUTE) == calendar2.get(Calendar.MINUTE)
			&& calendar1.get(Calendar.SECOND) == calendar2.get(Calendar.SECOND)
			&& calendar1.get(Calendar.MILLISECOND) == calendar2.get(Calendar.MILLISECOND);
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		Calendar calendar = java.util.Calendar.getInstance();
		calendar.setTime( (java.util.Date) x );
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get(Calendar.HOUR_OF_DAY);
		hashCode = 31 * hashCode + calendar.get(Calendar.MINUTE);
		hashCode = 31 * hashCode + calendar.get(Calendar.SECOND);
		hashCode = 31 * hashCode + calendar.get(Calendar.MILLISECOND);
		return hashCode;
	}

	public Object deepCopyNotNull(Object value) {
		return  new Time( ( (java.util.Date) value ).getTime() );
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return '\'' + new Time( ( (java.util.Date) value ).getTime() ).toString() + '\'';
	}

	public Object fromStringValue(String xml) throws HibernateException {
		try {
			return new SimpleDateFormat(TIME_FORMAT).parse(xml);
		}
		catch (ParseException pe) {
			throw new HibernateException("could not parse XML", pe);
		}
	}

}





