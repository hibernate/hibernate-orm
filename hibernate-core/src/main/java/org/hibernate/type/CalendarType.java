/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarType
		extends AbstractSingleColumnStandardBasicType<Calendar>
		implements VersionType<Calendar> {

	public static final CalendarType INSTANCE = new CalendarType();

	public CalendarType() {
		super( TimestampTypeDescriptor.INSTANCE, CalendarTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "calendar";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Calendar.class.getName(), GregorianCalendar.class.getName() };
	}

	public Calendar next(Calendar current, SessionImplementor session) {
		return seed( session );
	}

	public Calendar seed(SessionImplementor session) {
		return Calendar.getInstance();
	}

	public Comparator<Calendar> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
}
