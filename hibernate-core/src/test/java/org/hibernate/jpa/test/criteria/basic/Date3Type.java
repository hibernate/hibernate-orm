/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.basic;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Francois Gerodez
 */
public class Date3Type implements CompositeUserType {

	@Override
	public String[] getPropertyNames() {
		return new String[] { "year", "month", "day" };
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[] { StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER };
	}

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		Date date = (Date) component;
		Calendar c = GregorianCalendar.getInstance();
		c.setTime( date );

		switch ( property ) {
			case 0:
				return c.get( Calendar.YEAR );
			case 1:
				return c.get( Calendar.MONTH );
			case 2:
				return c.get( Calendar.DAY_OF_MONTH );
		}

		throw new HibernateException( "Invalid property provided" );
	}

	@Override
	public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
		Date date = (Date) component;
		Calendar c = GregorianCalendar.getInstance();
		c.setTime( date );

		switch ( property ) {
			case 0:
				c.set( Calendar.YEAR, (Integer) value );
			case 1:
				c.set( Calendar.MONTH, (Integer) value );
			case 2:
				c.set( Calendar.DAY_OF_MONTH, (Integer) value );
			default:
				throw new HibernateException( "Invalid property provided" );
		}
	}

	@Override
	public Class returnedClass() {
		return Date.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y )
			return true;
		if ( x == null || y == null )
			return false;
		Date dx = (Date) x;
		Date dy = (Date) y;

		return dx.equals( dy );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		Date dx = (Date) x;
		return dx.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		Date date = new Date();
		Calendar c = GregorianCalendar.getInstance();
		c.setTime( date );

		Integer year = StandardBasicTypes.INTEGER.nullSafeGet( rs, names[0], session );
		Integer month = StandardBasicTypes.INTEGER.nullSafeGet( rs, names[1], session );
		Integer day = StandardBasicTypes.INTEGER.nullSafeGet( rs, names[2], session );

		c.set( year, month, day );

		return date;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		Date date = new Date();
		Calendar c = GregorianCalendar.getInstance();
		c.setTime( date );

		StandardBasicTypes.INTEGER.nullSafeSet( st, c.get( Calendar.YEAR ), index, session );
		StandardBasicTypes.INTEGER.nullSafeSet( st, c.get( Calendar.MONTH ), index + 1, session );
		StandardBasicTypes.INTEGER.nullSafeSet( st, c.get( Calendar.DAY_OF_MONTH ), index + 2, session );
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		if ( value == null )
			return null;

		Date date = (Date) value;
		return date.clone();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
		return (Serializable) deepCopy( value );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return deepCopy( cached );
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return deepCopy( original ); // TODO: improve
	}
}
