/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cut;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * Class for testing composite user types with more than two fields.
 *
 * @author Etienne Miret
 */
public class CompositeDateTimeUserType implements CompositeUserType {

	@Override
	public String[] getPropertyNames() {
		return new String[] { "year", "month", "day", "hour", "minute", "second" };
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[] { StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER,
				StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER };
	}

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		final CompositeDateTime dateTime = (CompositeDateTime) component;
		switch ( property ) {
			case 0:
				return dateTime.getYear();

			case 1:
				return dateTime.getMonth();

			case 2:
				return dateTime.getDay();

			case 3:
				return dateTime.getHour();

			case 4:
				return dateTime.getMinute();

			case 5:
				return dateTime.getSecond();

			default:
				throw new HibernateException( "This type has only 6 fields." );
		}
	}

	@Override
	public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
	}

	@Override
	public Class<CompositeDateTime> returnedClass() {
		return CompositeDateTime.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return x == null ? y == null : x.equals( y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		final Integer year = rs.getObject( names[0], Integer.class );
		final Integer month = rs.getObject( names[1], Integer.class );
		final Integer day = rs.getObject( names[2], Integer.class );
		final Integer hour = rs.getObject( names[3], Integer.class );
		final Integer minute = rs.getObject( names[4], Integer.class );
		final Integer second = rs.getObject( names[5], Integer.class );
		if ( year == null && month == null && day == null && hour == null && minute == null && second == null ) {
			return null;
		} else {
			return new CompositeDateTime( year, month, day, hour, minute, second );
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			for (int i = 0; i < 6; i++ ) {
				st.setNull( index + i, Types.INTEGER );
			}
		} else {
			final CompositeDateTime dateTime = (CompositeDateTime) value;
			st.setObject( index, dateTime.getYear() );
			st.setObject( index + 1, dateTime.getMonth() );
			st.setObject( index + 2, dateTime.getDay() );
			st.setObject( index + 3, dateTime.getHour() );
			st.setObject( index + 4, dateTime.getMinute() );
			st.setObject( index + 5, dateTime.getSecond() );
		}
	}

	@Override
	public CompositeDateTime deepCopy(Object value) throws HibernateException {
		return value == null ? null : new CompositeDateTime( (CompositeDateTime) value );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
		return deepCopy( value );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return deepCopy( cached );
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return deepCopy( original );
	}

}
