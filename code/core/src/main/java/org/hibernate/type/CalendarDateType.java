//$Id: CalendarDateType.java 8761 2005-12-06 04:34:23Z oneovthafew $
package org.hibernate.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.util.CalendarComparator;

/**
 * <tt>calendar_date</tt>: A type mapping for a <tt>Calendar</tt>
 * object that represents a date.
 * @author Gavin King
 */
public class CalendarDateType extends MutableType {

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {

		Date date = rs.getDate(name);
		if (date!=null) {
			Calendar cal = new GregorianCalendar();
			cal.setTime(date);
			return cal;
		}
		else {
			return null;
		}

	}

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		final Calendar cal = (Calendar) value;
		//st.setDate( index,  new Date( cal.getTimeInMillis() ), cal ); //JDK 1.5 only
		st.setDate( index,  new Date( cal.getTime().getTime() ), cal );
	}

	public int sqlType() {
		return Types.DATE;
	}

	public String toString(Object value) throws HibernateException {
		return Hibernate.DATE.toString( ( (Calendar) value ).getTime() );
	}

	public Object fromStringValue(String xml) throws HibernateException {
		Calendar result = new GregorianCalendar();
		result.setTime( ( (java.util.Date) Hibernate.DATE.fromStringValue(xml) ) );
		return result;
	}

	public Object deepCopyNotNull(Object value)  {
		return ( (Calendar) value ).clone();
	}

	public Class getReturnedClass() {
		return Calendar.class;
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		return CalendarComparator.INSTANCE.compare(x, y);
	}

	public boolean isEqual(Object x, Object y)  {
		if (x==y) return true;
		if (x==null || y==null) return false;

		Calendar calendar1 = (Calendar) x;
		Calendar calendar2 = (Calendar) y;

		return calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH)
			&& calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
			&& calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		Calendar calendar = (Calendar) x;
		int hashCode = 1;
		hashCode = 31 * hashCode + calendar.get(Calendar.DAY_OF_MONTH);
		hashCode = 31 * hashCode + calendar.get(Calendar.MONTH);
		hashCode = 31 * hashCode + calendar.get(Calendar.YEAR);
		return hashCode;
	}

	public String getName() {
		return "calendar_date";
	}

}
