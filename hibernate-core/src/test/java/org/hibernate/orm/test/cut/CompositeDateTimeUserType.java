/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;

/**
 * Class for testing composite user types with more than two fields.
 *
 * @author Etienne Miret
 */
public class CompositeDateTimeUserType implements CompositeUserType<CompositeDateTime> {

	@Override
	public Object getPropertyValue(CompositeDateTime dateTime, int property) throws HibernateException {
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
	public CompositeDateTime instantiate(ValueAccess values, SessionFactoryImplementor sessionFactory) {
		Integer year = values.getValue( 0, Integer.class );
		Integer month = values.getValue( 1, Integer.class );
		Integer day = values.getValue( 2, Integer.class );
		Integer hour = values.getValue( 3, Integer.class );
		Integer minute = values.getValue( 4, Integer.class );
		Integer second = values.getValue( 5, Integer.class );
		if ( year == null && month == null && day == null && hour == null && minute == null && second == null ) {
			return null;
		}
		return new CompositeDateTime( year, month, day, hour, minute, second );
	}

	@Override
	public Class<?> embeddable() {
		return CompositeDateTime.class;
	}

	@Override
	public Class<CompositeDateTime> returnedClass() {
		return CompositeDateTime.class;
	}

	@Override
	public boolean equals(CompositeDateTime x, CompositeDateTime y) throws HibernateException {
		return x == null ? y == null : x.equals( y );
	}

	@Override
	public int hashCode(CompositeDateTime x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public CompositeDateTime deepCopy(CompositeDateTime value) throws HibernateException {
		return value == null ? null : new CompositeDateTime( value );
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(CompositeDateTime value) {
		return deepCopy( value );
	}

	@Override
	public CompositeDateTime assemble(Serializable cached, Object owner) {
		return deepCopy( (CompositeDateTime) cached );
	}

	@Override
	public CompositeDateTime replace(CompositeDateTime original, CompositeDateTime managed, Object owner) {
		return deepCopy( original );
	}
}
